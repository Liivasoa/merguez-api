package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.group.domain.model.SessionPayment;
import mg.msys.merguez_api.user.domain.model.UserId;

class CloseSessionUseCaseTest {

    @Test
    void adminClosesSessionAndLogsEachMembersResultAfterSaving() {
        Fixture fixture = new Fixture();
        fixture.assertSuccessfulClosure(fixture.admin);
    }

    @Test
    void memberClosesSessionAndLogsEachMembersResultAfterSaving() {
        Fixture fixture = new Fixture();
        assertTrue(fixture.group.isMember(fixture.member));
        assertFalse(fixture.group.isAdmin(fixture.member));
        fixture.assertSuccessfulClosure(fixture.member);
    }

    @Test
    void propagatesPersistenceFailureWithoutRequestingLogs() {
        Fixture fixture = new Fixture();
        RuntimeException failure = new RuntimeException("Persistence failed");
        doThrow(failure).when(fixture.persistence).save(fixture.occurrence);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> fixture.close(fixture.admin));

        assertSame(failure, thrown);
        verifyNoInteractions(fixture.logger);
    }

    @Test
    void propagatesLoggingFailureAfterSavingTheClosure() {
        Fixture fixture = new Fixture();
        RuntimeException failure = new RuntimeException("Logging failed");
        doThrow(failure).when(fixture.logger).log(any(), any());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> fixture.close(fixture.member));

        assertSame(failure, thrown);
        InOrder order = inOrder(fixture.persistence, fixture.logger);
        order.verify(fixture.persistence).save(fixture.occurrence);
        order.verify(fixture.logger).log(any(), any());
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static final class Fixture {
        final UserId admin = userId();
        final UserId member = userId();
        final UserId inactiveMember = userId();
        final Group group = Group.create("Training", admin, List.of(member, inactiveMember));
        final Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        final SessionOccurrence occurrence = group.startSession(admin, session);
        final Instant startedAt = occurrence.startedAt();
        final LoadGroupWithActiveOccurrencePort loader = mock(LoadGroupWithActiveOccurrencePort.class);
        final SaveClosedSessionOccurrencePort persistence = mock(SaveClosedSessionOccurrencePort.class);
        final LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        final CloseSessionUseCase useCase = new CloseSessionUseCase(loader, persistence, logger);
        final Round closedRound;

        Fixture() {
            Combat combat = new Combat(List.of(admin, member), Optional.empty());
            Round round = group.addRound(admin, occurrence, List.of(combat));
            closedRound = group.closeRound(admin, round,
                    List.of(new Combat(combat.participants(), Optional.of(admin))));
            when(loader.loadByGroupId(group.id())).thenReturn(Optional.of(group));
            doAnswer(invocation -> {
                SessionOccurrence saved = invocation.getArgument(0);
                assertSame(occurrence, saved);
                assertTrue(saved.isClosed());
                return null;
            }).when(persistence).save(any());
        }

        SessionClosureResult close(UserId requester) {
            return useCase.execute(new CloseSessionCommand(group.id(), requester));
        }

        void assertSuccessfulClosure(UserId requester) {
            assertFalse(occurrence.isClosed());

            SessionClosureResult result = close(requester);

            assertAll(
                    () -> assertSame(occurrence, result.occurrence()),
                    () -> assertTrue(result.occurrence().isClosed()),
                    () -> assertEquals(group.id(), result.occurrence().groupId()),
                    () -> assertEquals(session.id(), result.occurrence().scheduleId()),
                    () -> assertEquals(startedAt, result.occurrence().startedAt()),
                    () -> assertTrue(group.activeOccurrences().isEmpty()),
                    () -> assertEquals(List.of(closedRound), group.rounds(occurrence)),
                    () -> assertEquals(Map.of(admin, 1, member, -1, inactiveMember, 0), result.netScores()),
                    () -> assertEquals(Set.of(new SessionPayment(member, admin, 1)), Set.copyOf(result.payments())),
                    () -> assertEquals(1, result.payments().size()));
            verify(loader).loadByGroupId(group.id());
            InOrder order = inOrder(persistence, logger);
            order.verify(persistence).save(same(result.occurrence()));
            order.verify(logger, times(3)).log(any(), same(result));
            verify(logger).log(admin, result);
            verify(logger).log(member, result);
            verify(logger).log(inactiveMember, result);
            verifyNoMoreInteractions(persistence, logger);
        }
    }
}

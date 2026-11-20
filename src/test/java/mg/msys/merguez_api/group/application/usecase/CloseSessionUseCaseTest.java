package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
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
        doThrow(failure).when(fixture.persistence).save(eq(fixture.occurrence), anyList());

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
        order.verify(fixture.persistence).save(eq(fixture.occurrence), eq(List.of(fixture.closedRound)));
        order.verify(fixture.logger).log(any(), any());
    }

    @Test
    void refusesToCloseDifferentScheduledSessionWithoutChangingActiveOccurrence() {
        UserId admin = userId();
        UserId member = userId();
        Group group = spy(Group.create("Training", admin, List.of(member)));
        Session activeSession = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        Session requestedSession = group.createSession(admin, "Later practice", DayOfWeek.WEDNESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, activeSession);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round round = group.addRound(admin, occurrence, List.of(combat));
        Combat result = new Combat(combat.participants(), Optional.of(admin));
        Round closedRound = group.closeRound(admin, round, List.of(result));
        var sessions = group.sessions();
        var members = group.members();
        var activeOccurrences = group.activeOccurrences();
        var rounds = group.rounds(occurrence);
        Instant startedAt = occurrence.startedAt();
        LoadGroupWithActiveOccurrencePort loader = mock(LoadGroupWithActiveOccurrencePort.class);
        GroupRepository groups = mock(GroupRepository.class);
        SaveClosedSessionOccurrencePort persistence = mock(SaveClosedSessionOccurrencePort.class);
        LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        CloseSessionUseCase useCase = new CloseSessionUseCase(loader, groups, persistence, logger);
        when(loader.loadByGroupId(group.id())).thenReturn(Optional.of(group));

        assertEquals(group.id(), activeSession.groupId());
        assertEquals(group.id(), requestedSession.groupId());
        assertNotEquals(activeSession.id(), requestedSession.id());
        assertEquals(activeSession.id(), occurrence.scheduleId());
        assertEquals(List.of(activeSession, requestedSession), sessions);
        assertEquals(List.of(occurrence), activeOccurrences);
        assertEquals(List.of(closedRound), rounds);
        assertEquals(RoundStatus.CLOSED, closedRound.status());
        assertEquals(List.of(result), closedRound.combats());
        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> useCase.execute(new CloseSessionCommand(group.id(), admin, requestedSession.id())));

        assertTrue(thrown.getMessage().toLowerCase(java.util.Locale.ROOT).contains("session"));
        assertTrue(thrown.getMessage().toLowerCase(java.util.Locale.ROOT).contains("active"));
        verify(loader).loadByGroupId(group.id());
        verify(group, never()).closeSession(nullable(UserId.class), nullable(SessionOccurrence.class));
        assertAll(
                () -> assertEquals(sessions, group.sessions()),
                () -> assertEquals(members, group.members()),
                () -> assertEquals(activeOccurrences, group.activeOccurrences()),
                () -> assertSame(occurrence, group.activeOccurrences().getFirst()),
                () -> assertEquals(rounds, group.rounds(occurrence)),
                () -> assertEquals(RoundStatus.CLOSED, closedRound.status()),
                () -> assertEquals(List.of(result), closedRound.combats()),
                () -> assertEquals(group.id(), occurrence.groupId()),
                () -> assertEquals(activeSession.id(), occurrence.scheduleId()),
                () -> assertEquals(startedAt, occurrence.startedAt()),
                () -> assertTrue(occurrence.isStarted()),
                () -> assertFalse(occurrence.isClosed()));
        verifyNoInteractions(persistence, logger);
    }

    @Test
    void propagatesDomainRefusalWhenNoOccurrenceIsActive() {
        RefusalFixture fixture = new RefusalFixture();
        when(fixture.loader.loadByGroupId(fixture.group.id())).thenReturn(Optional.empty());
        var sessions = fixture.group.sessions();
        var members = fixture.group.members();
        assertTrue(fixture.group.activeOccurrences().isEmpty());

        IllegalStateException thrown = assertThrows(IllegalStateException.class, fixture::close);

        assertSame(fixture.domainFailure.get(), thrown);
        assertEquals("Session occurrence must be in progress", thrown.getMessage());
        verify(fixture.loader).loadByGroupId(fixture.group.id());
        verify(fixture.groups).findById(fixture.group.id());
        verify(fixture.group).closeSession(fixture.admin, null);
        assertTrue(fixture.group.activeOccurrences().isEmpty());
        assertEquals(sessions, fixture.group.sessions());
        assertEquals(members, fixture.group.members());
        verifyNoInteractions(fixture.persistence, fixture.logger);
    }

    @Test
    void propagatesDomainRefusalForActiveOccurrenceWithoutRounds() {
        RefusalFixture fixture = new RefusalFixture();
        SessionOccurrence occurrence = fixture.group.startSession(fixture.admin, fixture.session);
        fixture.loadActiveGroup();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, fixture::close);

        assertSame(fixture.domainFailure.get(), thrown);
        assertEquals("A session must contain at least one round", thrown.getMessage());
        verify(fixture.group).closeSession(fixture.admin, occurrence);
        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());
        assertEquals(List.of(occurrence), fixture.group.activeOccurrences());
        assertTrue(fixture.group.rounds(occurrence).isEmpty());
        verifyNoInteractions(fixture.persistence, fixture.logger);
    }

    @Test
    void propagatesDomainRefusalWhenAnyRoundIsStillOpen() {
        RefusalFixture fixture = new RefusalFixture();
        SessionOccurrence occurrence = fixture.group.startSession(fixture.admin, fixture.session);
        Combat combat = new Combat(List.of(fixture.admin, fixture.member), Optional.empty());
        Round firstRound = fixture.group.addRound(fixture.admin, occurrence, List.of(combat));
        Combat result = new Combat(combat.participants(), Optional.of(fixture.admin));
        Round closedRound = fixture.group.closeRound(fixture.admin, firstRound, List.of(result));
        Round openRound = fixture.group.addRound(fixture.admin, occurrence, List.of(combat));
        fixture.loadActiveGroup();

        IllegalStateException thrown = assertThrows(IllegalStateException.class, fixture::close);

        assertSame(fixture.domainFailure.get(), thrown);
        assertEquals("All rounds must be closed before closing the session", thrown.getMessage());
        verify(fixture.group).closeSession(fixture.admin, occurrence);
        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());
        assertEquals(List.of(occurrence), fixture.group.activeOccurrences());
        assertEquals(List.of(closedRound, openRound), fixture.group.rounds(occurrence));
        assertEquals(RoundStatus.CLOSED, closedRound.status());
        assertEquals(List.of(result), closedRound.combats());
        assertEquals(RoundStatus.IN_PROGRESS, openRound.status());
        assertEquals(List.of(combat), openRound.combats());
        verifyNoInteractions(fixture.persistence, fixture.logger);
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static final class RefusalFixture {
        final UserId admin = userId();
        final UserId member = userId();
        final Group group = spy(Group.create("Training", admin, List.of(member)));
        final Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        final LoadGroupWithActiveOccurrencePort loader = mock(LoadGroupWithActiveOccurrencePort.class);
        final GroupRepository groups = mock(GroupRepository.class);
        final SaveClosedSessionOccurrencePort persistence = mock(SaveClosedSessionOccurrencePort.class);
        final LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        final AtomicReference<RuntimeException> domainFailure = new AtomicReference<>();
        final CloseSessionUseCase useCase = new CloseSessionUseCase(loader, groups, persistence, logger);

        RefusalFixture() {
            when(groups.findById(group.id())).thenReturn(Optional.of(group));
            doAnswer(invocation -> {
                try {
                    return invocation.callRealMethod();
                } catch (RuntimeException failure) {
                    domainFailure.set(failure);
                    throw failure;
                }
            }).when(group).closeSession(any(), nullable(SessionOccurrence.class));
        }

        void loadActiveGroup() {
            when(loader.loadByGroupId(group.id())).thenReturn(Optional.of(group));
        }

        SessionClosureResult close() {
            return useCase.execute(new CloseSessionCommand(group.id(), admin, session.id()));
        }
    }

    private static final class Fixture {
        final UserId admin = userId();
        final UserId member = userId();
        final UserId inactiveMember = userId();
        final Group group = spy(Group.create("Training", admin, List.of(member, inactiveMember)));
        final Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        final SessionOccurrence occurrence = group.startSession(admin, session);
        final Instant startedAt = occurrence.startedAt();
        final LoadGroupWithActiveOccurrencePort loader = mock(LoadGroupWithActiveOccurrencePort.class);
        final GroupRepository groups = mock(GroupRepository.class);
        final SaveClosedSessionOccurrencePort persistence = mock(SaveClosedSessionOccurrencePort.class);
        final LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        final CloseSessionUseCase useCase = new CloseSessionUseCase(loader, groups, persistence, logger);
        final Round closedRound;

        Fixture() {
            Combat combat = new Combat(List.of(admin, member), Optional.empty());
            Round round = group.addRound(admin, occurrence, List.of(combat));
            closedRound = group.closeRound(admin, round,
                    List.of(new Combat(combat.participants(), Optional.of(admin))));
            when(loader.loadByGroupId(group.id())).thenReturn(Optional.of(group));
            when(groups.findById(group.id())).thenReturn(Optional.of(group));
            doAnswer(invocation -> {
                SessionOccurrence saved = invocation.getArgument(0);
                List<Round> snapshot = invocation.getArgument(1);
                assertSame(occurrence, saved);
                assertTrue(saved.isClosed());
                assertEquals(List.of(closedRound), snapshot);
                assertThrows(UnsupportedOperationException.class, () -> snapshot.add(closedRound));
                return null;
            }).when(persistence).save(any(), anyList());
        }

        SessionClosureResult close(UserId requester) {
            return useCase.execute(new CloseSessionCommand(group.id(), requester, session.id()));
        }

        void assertSuccessfulClosure(UserId requester) {
            assertFalse(occurrence.isClosed());

            SessionClosureResult result = close(requester);

            InOrder snapshotOrder = inOrder(group, persistence);
            snapshotOrder.verify(group).rounds(occurrence);
            snapshotOrder.verify(group).closeSession(requester, occurrence);
            snapshotOrder.verify(persistence).save(same(result.occurrence()), eq(List.of(closedRound)));
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
            order.verify(persistence).save(same(result.occurrence()), eq(List.of(closedRound)));
            order.verify(logger, times(3)).log(any(), same(result));
            verify(logger).log(admin, result);
            verify(logger).log(member, result);
            verify(logger).log(inactiveMember, result);
            verifyNoMoreInteractions(persistence, logger);
        }
    }
}

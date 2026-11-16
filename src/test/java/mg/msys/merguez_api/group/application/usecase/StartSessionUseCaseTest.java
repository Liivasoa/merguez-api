package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

class StartSessionUseCaseTest {

    @ParameterizedTest
    @ValueSource(strings = {"admin", "member"})
    void startsScheduledSessionAndPersistsDomainOccurrence(String requesterRole) {
        UserId admin = userId();
        UserId member = userId();
        Group group = Group.restore(GroupId.random(), "Dojo du mardi", null, admin, List.of(member));
        UserId requester = "admin".equals(requesterRole) ? admin : member;
        Session scheduledSession =
                group.createSession(admin, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        InMemoryGroupRepository groupRepository = new InMemoryGroupRepository(group);
        InMemorySessionOccurrenceRepository occurrenceRepository = new InMemorySessionOccurrenceRepository();
        StartSessionUseCase useCase = new StartSessionUseCase(groupRepository, occurrenceRepository);
        StartSessionCommand command = new StartSessionCommand(group.id(), requester, scheduledSession);

        SessionOccurrence result = useCase.execute(command);

        assertAll(
                () -> assertEquals(group.id(), result.groupId()),
                () -> assertEquals(scheduledSession.id(), result.scheduleId()),
                () -> assertTrue(result.isStarted()),
                () -> assertNotSame(scheduledSession, result),
                () -> assertEquals(List.of(scheduledSession), group.sessions()),
                () -> assertEquals(1, occurrenceRepository.savedOccurrences.size()),
                () -> assertSame(result, occurrenceRepository.savedOccurrences.getFirst()),
                () -> assertEquals(result.startedAt(), occurrenceRepository.savedOccurrences.getFirst().startedAt()),
                () -> assertEquals(List.of(), groupRepository.savedGroups));
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static class InMemoryGroupRepository implements GroupRepository {

        private final Group group;
        private final List<Group> savedGroups = new ArrayList<>();

        private InMemoryGroupRepository(Group group) {
            this.group = group;
        }

        @Override
        public void save(Group group) {
            savedGroups.add(group);
        }

        @Override
        public Optional<Group> findById(GroupId id) {
            return group.id().equals(id) ? Optional.of(group) : Optional.empty();
        }
    }

    private static class InMemorySessionOccurrenceRepository implements SessionOccurrenceRepository {

        private final List<SessionOccurrence> savedOccurrences = new ArrayList<>();

        @Override
        public void save(SessionOccurrence occurrence) {
            savedOccurrences.add(occurrence);
        }
    }
}

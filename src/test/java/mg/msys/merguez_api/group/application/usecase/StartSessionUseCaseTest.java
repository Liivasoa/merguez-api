package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import mg.msys.merguez_api.group.application.command.AddRoundCommand;
import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
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
        StartSessionCommand command = new StartSessionCommand(group.id(), requester, scheduledSession.id());

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

    @Test
    void resolvesScheduledSessionIdentifierFromLoadedGroup() {
        UserId admin = userId();
        UserId member = userId();
        Group group = Group.restore(GroupId.random(), "Training group", null, admin, List.of(member));
        Session firstSchedule = group.createSession(
                admin, "First session", DayOfWeek.MONDAY, LocalTime.of(10, 0));
        Session selectedSchedule = group.createSession(
                admin, "Selected session", DayOfWeek.TUESDAY, LocalTime.of(11, 0));
        InMemoryGroupRepository groupRepository = new InMemoryGroupRepository(group);
        InMemorySessionOccurrenceRepository occurrenceRepository = new InMemorySessionOccurrenceRepository();
        StartSessionUseCase useCase = new StartSessionUseCase(groupRepository, occurrenceRepository);

        SessionOccurrence result = useCase.execute(
                new StartSessionCommand(group.id(), member, selectedSchedule.id()));

        assertAll(
                () -> assertEquals(group.id(), result.groupId()),
                () -> assertEquals(selectedSchedule.id(), result.scheduleId()),
                () -> assertTrue(result.isStarted()),
                () -> assertEquals(List.of(firstSchedule, selectedSchedule), group.sessions()),
                () -> assertEquals(1, group.activeOccurrences().size()),
                () -> assertSame(result, group.activeOccurrences().getFirst()),
                () -> assertEquals(1, occurrenceRepository.savedOccurrences.size()),
                () -> assertSame(result, occurrenceRepository.savedOccurrences.getFirst()),
                () -> assertEquals(List.of(), groupRepository.savedGroups));
    }

    @Test
    void rejectsAnotherStartAfterReloadButAllowsStartInAnotherGroup() {
        UserId firstAdmin = userId();
        UserId firstMember = userId();
        GroupId firstGroupId = GroupId.random();
        Group originalGroup = Group.restore(firstGroupId, "First group", null, firstAdmin, List.of(firstMember));
        Session firstSchedule = originalGroup.createSession(
                firstAdmin, "First session", DayOfWeek.MONDAY, LocalTime.of(10, 0));
        Session originalAnotherSchedule = originalGroup.createSession(
                firstAdmin, "Another session", DayOfWeek.TUESDAY, LocalTime.of(10, 0));
        InMemorySessionOccurrenceRepository occurrenceRepository = new InMemorySessionOccurrenceRepository();
        StartSessionUseCase initialUseCase = new StartSessionUseCase(
                new InMemoryGroupRepository(originalGroup), occurrenceRepository);
        SessionOccurrence firstOccurrence = initialUseCase.execute(
                new StartSessionCommand(firstGroupId, firstMember, firstSchedule.id()));

        Group freshGroup = Group.restore(
                firstGroupId, "First group", null, firstAdmin, List.of(firstMember), List.of(originalAnotherSchedule));
        Session anotherSchedule = freshGroup.sessions().getFirst();
        UserId secondAdmin = userId();
        Group secondGroup = Group.restore(GroupId.random(), "Second group", null, secondAdmin, List.of(userId()));
        Session secondSchedule = secondGroup.createSession(
                secondAdmin, "Second group session", DayOfWeek.MONDAY, LocalTime.of(10, 0));
        InMemoryGroupRepository groupRepository = new InMemoryGroupRepository(freshGroup, secondGroup);
        StartSessionUseCase useCase = new StartSessionUseCase(groupRepository, occurrenceRepository);

        assertSame(freshGroup, groupRepository.findById(firstGroupId).orElseThrow());
        assertNotSame(originalGroup, freshGroup);
        assertTrue(freshGroup.activeOccurrences().isEmpty());
        assertTrue(secondGroup.activeOccurrences().isEmpty());
        assertEquals(originalAnotherSchedule, anotherSchedule);
        assertTrue(occurrenceRepository.existsStartedByGroupId(firstGroupId));
        assertFalse(occurrenceRepository.existsStartedByGroupId(secondGroup.id()));
        assertEquals(List.of(firstOccurrence), occurrenceRepository.occurrencesFor(firstGroupId));
        assertEquals(List.of(), occurrenceRepository.occurrencesFor(secondGroup.id()));
        assertEquals(1, occurrenceRepository.savedOccurrences.size());

        assertAll(
                () -> assertThrows(RuntimeException.class, () -> useCase.execute(
                        new StartSessionCommand(firstGroupId, firstMember, anotherSchedule.id()))),
                () -> assertEquals(List.of(firstOccurrence), occurrenceRepository.occurrencesFor(firstGroupId)),
                () -> assertEquals(1, occurrenceRepository.savedOccurrences.size()),
                () -> {
                    SessionOccurrence result = useCase.execute(
                            new StartSessionCommand(secondGroup.id(), secondAdmin, secondSchedule.id()));
                    assertAll(
                            () -> assertEquals(secondGroup.id(), result.groupId()),
                            () -> assertEquals(secondSchedule.id(), result.scheduleId()),
                            () -> assertTrue(result.isStarted()),
                            () -> assertEquals(List.of(result), occurrenceRepository.occurrencesFor(secondGroup.id())),
                            () -> assertSame(result, occurrenceRepository.occurrencesFor(secondGroup.id()).getFirst()));
                },
                () -> assertEquals(List.of(firstOccurrence), occurrenceRepository.occurrencesFor(firstGroupId)),
                () -> assertEquals(2, occurrenceRepository.savedOccurrences.size()),
                () -> assertEquals(List.of(), groupRepository.savedGroups));
    }

    @Test
    void createsAndPersistsInitialRoundFromReloadedActiveOccurrence() {
        UserId admin = userId();
        UserId firstMember = userId();
        UserId secondMember = userId();
        GroupId groupId = GroupId.random();
        Group initialGroup = Group.restore(groupId, "Training group", null, admin, List.of(firstMember, secondMember));
        Session session = initialGroup.createSession(
                admin, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence persistedOccurrence = initialGroup.startSession(admin, session);
        InMemoryRoundRepository roundRepository = new InMemoryRoundRepository();
        ReloadingGroupRepository groupRepository = new ReloadingGroupRepository(
                groupId,
                "Training group",
                admin,
                List.of(firstMember, secondMember),
                List.of(session),
                persistedOccurrence,
                roundRepository);
        Group loadedGroup = groupRepository.findById(groupId).orElseThrow();
        assertNotSame(initialGroup, loadedGroup);
        assertEquals(List.of(persistedOccurrence), loadedGroup.activeOccurrences());
        assertEquals(List.of(), loadedGroup.rounds(persistedOccurrence));

        UserId requester = firstMember;
        UserId winner = secondMember;
        Combat combat = new Combat(List.of(firstMember, secondMember), Optional.of(winner));
        AddRoundUseCase useCase = new AddRoundUseCase(groupRepository, roundRepository);

        Round result = useCase.execute(new AddRoundCommand(groupId, requester, List.of(combat)));

        Group reloadedGroup = groupRepository.findById(groupId).orElseThrow();
        Round persistedRound = roundRepository.savedRounds.getFirst();
        assertAll(
                () -> assertEquals(persistedOccurrence, result.occurrence()),
                () -> assertEquals(1, result.number()),
                () -> assertEquals(List.of(combat), result.combats()),
                () -> assertEquals(Optional.of(winner), result.combats().getFirst().winner()),
                () -> assertEquals(1, roundRepository.savedRounds.size()),
                () -> assertSame(result, persistedRound),
                () -> assertEquals(persistedOccurrence, persistedRound.occurrence()),
                () -> assertEquals(Optional.of(winner), persistedRound.combats().getFirst().winner()),
                () -> assertNotSame(loadedGroup, reloadedGroup),
                () -> assertEquals(List.of(result), reloadedGroup.rounds(persistedOccurrence)));
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private static class InMemoryGroupRepository implements GroupRepository {

        private final List<Group> groups;
        private final List<Group> savedGroups = new ArrayList<>();

        private InMemoryGroupRepository(Group... groups) {
            this.groups = List.of(groups);
        }

        @Override
        public void save(Group group) {
            savedGroups.add(group);
        }

        @Override
        public Optional<Group> findById(GroupId id) {
            return groups.stream().filter(group -> group.id().equals(id)).findFirst();
        }
    }

    private static class InMemorySessionOccurrenceRepository implements SessionOccurrenceRepository {

        private final List<SessionOccurrence> savedOccurrences = new ArrayList<>();

        @Override
        public void save(SessionOccurrence occurrence) {
            savedOccurrences.add(occurrence);
        }

        public boolean existsStartedByGroupId(GroupId groupId) {
            return savedOccurrences.stream()
                    .anyMatch(occurrence -> occurrence.groupId().equals(groupId) && occurrence.isStarted());
        }

        private List<SessionOccurrence> occurrencesFor(GroupId groupId) {
            return savedOccurrences.stream().filter(occurrence -> occurrence.groupId().equals(groupId)).toList();
        }
    }

    private static class ReloadingGroupRepository implements GroupRepository {

        private final GroupId groupId;
        private final String name;
        private final UserId admin;
        private final List<UserId> participants;
        private final List<Session> sessions;
        private final SessionOccurrence activeOccurrence;
        private final RoundRepository roundRepository;

        private ReloadingGroupRepository(
                GroupId groupId,
                String name,
                UserId admin,
                List<UserId> participants,
                List<Session> sessions,
                SessionOccurrence activeOccurrence,
                RoundRepository roundRepository) {
            this.groupId = groupId;
            this.name = name;
            this.admin = admin;
            this.participants = participants;
            this.sessions = sessions;
            this.activeOccurrence = activeOccurrence;
            this.roundRepository = roundRepository;
        }

        @Override
        public void save(Group group) {
            throw new AssertionError("Adding a round should persist the round, not an in-memory group instance");
        }

        @Override
        public Optional<Group> findById(GroupId id) {
            if (!groupId.equals(id)) {
                return Optional.empty();
            }
            Group restoredGroup = Group.restore(
                    groupId,
                    name,
                    null,
                    admin,
                    participants,
                    sessions,
                    activeOccurrence,
                    roundRepository.findByOccurrence(activeOccurrence));
            return Optional.of(restoredGroup);
        }
    }

    private static class InMemoryRoundRepository implements RoundRepository {

        private final List<Round> savedRounds = new ArrayList<>();

        @Override
        public void save(Round round) {
            savedRounds.add(round);
        }

        @Override
        public List<Round> findByOccurrence(SessionOccurrence occurrence) {
            return savedRounds.stream()
                    .filter(round -> round.occurrence().equals(occurrence))
                    .toList();
        }
    }
}

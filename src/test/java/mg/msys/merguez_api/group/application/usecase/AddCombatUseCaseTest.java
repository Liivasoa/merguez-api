package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.application.command.AddCombatCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

class AddCombatServiceTest {

    @Test
    void addsCombatToPersistedRoundAndReturnsUpdatedRoundAfterReload() {
        Scenario scenario = scenario();
        Combat availableCombat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.of(scenario.thirdMember()));
        AddCombatService useCase = new AddCombatService(scenario.groupLoader(), scenario.roundRepository());

        Round result = useCase.execute(new AddCombatCommand(
                scenario.groupId(), scenario.requester(), scenario.existingRound().number(), availableCombat));

        Round savedRound = scenario.roundRepository().persistedRounds().getFirst();
        Group reloadedGroup = scenario.groupLoader().loadByGroupId(scenario.groupId()).orElseThrow();
        Round reloadedRound = reloadedGroup.rounds(reloadedGroup.activeOccurrences().getFirst()).getFirst();
        List<Combat> expectedCombats = List.of(scenario.existingCombat(), availableCombat);
        assertAll(
                () -> assertEquals(scenario.existingRound().number(), result.number()),
                () -> assertEquals(scenario.occurrence(), result.occurrence()),
                () -> assertEquals(expectedCombats, result.combats()),
                () -> assertEquals(Optional.of(scenario.thirdMember()), result.combats().get(1).winner()),
                () -> assertEquals(1, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(1, scenario.roundRepository().persistedRounds().size()),
                () -> assertNotSame(result, savedRound),
                () -> assertEquals(expectedCombats, savedRound.combats()),
                () -> assertEquals(1, reloadedGroup.rounds(reloadedGroup.activeOccurrences().getFirst()).size()),
                () -> assertEquals(result.number(), reloadedRound.number()),
                () -> assertEquals(expectedCombats, reloadedRound.combats()));
    }

    @Test
    void propagatesDomainRefusalWithoutSavingAnUpdatedRound() {
        Scenario scenario = scenario();
        Combat combatWithAssignedParticipant = new Combat(
                List.of(scenario.firstMember(), scenario.thirdMember()), Optional.empty());
        AddCombatService useCase = new AddCombatService(scenario.groupLoader(), scenario.roundRepository());

        RoundOperationRefusedException exception = assertThrowsExactly(
                RoundOperationRefusedException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(),
                        scenario.requester(),
                        scenario.existingRound().number(),
                        combatWithAssignedParticipant)));

        Group reloadedGroup = scenario.groupLoader().loadByGroupId(scenario.groupId()).orElseThrow();
        Round reloadedRound = reloadedGroup.rounds(reloadedGroup.activeOccurrences().getFirst()).getFirst();
        assertAll(
                () -> assertEquals(IllegalArgumentException.class, exception.getCause().getClass()),
                () -> assertEquals(
                        "A participant can only appear in one combat per round", exception.getCause().getMessage()),
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(1, scenario.roundRepository().persistedRounds().size()),
                () -> assertEquals(List.of(scenario.existingCombat()), reloadedRound.combats()));
    }

    @Test
    void refusesMissingSelectedRoundWithoutSaving() {
        Scenario scenario = scenario();
        Combat availableCombat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.empty());
        AddCombatService useCase = new AddCombatService(scenario.groupLoader(), scenario.roundRepository());

        RoundOperationRefusedException exception = assertThrowsExactly(
                RoundOperationRefusedException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(),
                        scenario.requester(),
                        scenario.existingRound().number() + 1,
                        availableCombat)));

        assertAll(
                () -> assertEquals(IllegalArgumentException.class, exception.getCause().getClass()),
                () -> assertEquals("Round not found", exception.getCause().getMessage()),
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(List.of(scenario.existingRound()), scenario.roundRepository().persistedRounds()));
    }

    @Test
    void refusesUnavailableActiveOccurrenceWithoutSaving() {
        Scenario scenario = scenario();
        LoadGroupWithActiveOccurrencePort groupLoader = ignored -> Optional.empty();
        AddCombatService useCase = new AddCombatService(groupLoader, scenario.roundRepository());
        Combat combat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.empty());

        RoundOperationRefusedException exception = assertThrowsExactly(
                RoundOperationRefusedException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(), scenario.requester(), scenario.existingRound().number(), combat)));

        assertAll(
                () -> assertEquals(IllegalStateException.class, exception.getCause().getClass()),
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(List.of(scenario.existingRound()), scenario.roundRepository().persistedRounds()));
    }

    @Test
    void treatsLoadedGroupWithoutActiveOccurrenceAsLoaderContractViolation() {
        Scenario scenario = scenario();
        Group loadedGroupWithoutOccurrence = Group.restore(
                scenario.groupId(),
                "Training group",
                null,
                scenario.firstMember(),
                List.of(scenario.secondMember(), scenario.thirdMember(), scenario.fourthMember()));
        LoadGroupWithActiveOccurrencePort groupLoader = ignored -> Optional.of(loadedGroupWithoutOccurrence);
        AddCombatService useCase = new AddCombatService(groupLoader, scenario.roundRepository());
        Combat combat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.empty());

        assertThrowsExactly(
                IllegalStateException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(), scenario.requester(), scenario.existingRound().number(), combat)));

        assertAll(
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(List.of(scenario.existingRound()), scenario.roundRepository().persistedRounds()));
    }

    @Test
    void propagatesRoundRepositoryFailureUnchanged() {
        Scenario scenario = scenario();
        IllegalStateException repositoryFailure = new IllegalStateException("Round storage is unavailable");
        RoundRepository failingRepository = mock(RoundRepository.class);
        doThrow(repositoryFailure).when(failingRepository).replace(any(Round.class), any(Round.class));
        AddCombatService useCase = new AddCombatService(scenario.groupLoader(), failingRepository);
        Combat availableCombat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.empty());

        IllegalStateException exception = assertThrowsExactly(
                IllegalStateException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(),
                        scenario.requester(),
                        scenario.existingRound().number(),
                        availableCombat)));

        assertAll(
                () -> assertSame(repositoryFailure, exception),
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(List.of(scenario.existingRound()), scenario.roundRepository().persistedRounds()));
    }

    @Test
    void propagatesGroupLoaderFailureUnchanged() {
        Scenario scenario = scenario();
        IllegalStateException loaderFailure = new IllegalStateException("Group data is corrupt");
        LoadGroupWithActiveOccurrencePort failingLoader = ignored -> {
            throw loaderFailure;
        };
        AddCombatService useCase = new AddCombatService(failingLoader, scenario.roundRepository());
        Combat availableCombat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.empty());

        IllegalStateException exception = assertThrowsExactly(
                IllegalStateException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(),
                        scenario.requester(),
                        scenario.existingRound().number(),
                        availableCombat)));

        assertAll(
                () -> assertSame(loaderFailure, exception),
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(List.of(scenario.existingRound()), scenario.roundRepository().persistedRounds()));
    }

    @Test
    void propagatesGroupAccessDenialUnchangedWithoutSaving() {
        Scenario scenario = scenario();
        AddCombatService useCase = new AddCombatService(scenario.groupLoader(), scenario.roundRepository());
        Combat availableCombat = new Combat(
                List.of(scenario.thirdMember(), scenario.fourthMember()), Optional.empty());

        GroupAccessDeniedException exception = assertThrowsExactly(
                GroupAccessDeniedException.class,
                () -> useCase.execute(new AddCombatCommand(
                        scenario.groupId(),
                        userId(),
                        scenario.existingRound().number(),
                        availableCombat)));

        assertAll(
                () -> assertEquals("Only group members can add combats", exception.getMessage()),
                () -> assertEquals(0, scenario.roundRepository().saveCalls().size()),
                () -> assertEquals(List.of(scenario.existingRound()), scenario.roundRepository().persistedRounds()));
    }

    private static Scenario scenario() {
        UserId admin = userId();
        UserId firstMember = userId();
        UserId secondMember = userId();
        UserId thirdMember = userId();
        UserId fourthMember = userId();
        GroupId groupId = GroupId.random();
        List<UserId> participants = List.of(firstMember, secondMember, thirdMember, fourthMember);
        Group initialGroup = Group.restore(groupId, "Training group", null, admin, participants);
        Session session = initialGroup.createSession(
                admin, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = initialGroup.startSession(admin, session);
        Combat existingCombat = new Combat(List.of(firstMember, secondMember), Optional.of(secondMember));
        Round existingRound = initialGroup.addRound(admin, occurrence, List.of(existingCombat));
        InMemoryRoundRepository roundRepository = new InMemoryRoundRepository();
        roundRepository.seed(existingRound);
        LoadGroupWithActiveOccurrencePort groupLoader = new ReloadingGroupLoader(
                groupId, "Training group", admin, participants, session, occurrence, roundRepository);
        return new Scenario(
                groupId,
                firstMember,
                secondMember,
                thirdMember,
                fourthMember,
                firstMember,
                occurrence,
                existingCombat,
                existingRound,
                groupLoader,
                roundRepository);
    }

    private static UserId userId() {
        return new UserId(UUID.randomUUID());
    }

    private record Scenario(
            GroupId groupId,
            UserId firstMember,
            UserId secondMember,
            UserId thirdMember,
            UserId fourthMember,
            UserId requester,
            SessionOccurrence occurrence,
            Combat existingCombat,
            Round existingRound,
            LoadGroupWithActiveOccurrencePort groupLoader,
            InMemoryRoundRepository roundRepository) {}

    private static class ReloadingGroupLoader implements LoadGroupWithActiveOccurrencePort {

        private final GroupId groupId;
        private final String name;
        private final UserId admin;
        private final List<UserId> participants;
        private final Session session;
        private final SessionOccurrence persistedOccurrence;
        private final InMemoryRoundRepository roundRepository;

        private ReloadingGroupLoader(
                GroupId groupId,
                String name,
                UserId admin,
                List<UserId> participants,
                Session session,
                SessionOccurrence persistedOccurrence,
                InMemoryRoundRepository roundRepository) {
            this.groupId = groupId;
            this.name = name;
            this.admin = admin;
            this.participants = participants;
            this.session = session;
            this.persistedOccurrence = persistedOccurrence;
            this.roundRepository = roundRepository;
        }

        @Override
        public Optional<Group> loadByGroupId(GroupId requestedGroupId) {
            if (!groupId.equals(requestedGroupId)) {
                return Optional.empty();
            }
            SessionOccurrence activeOccurrence = new SessionOccurrence(
                    persistedOccurrence.groupId(),
                    persistedOccurrence.scheduleId(),
                    persistedOccurrence.startedAt());
            Group restoredGroup = Group.restore(
                    groupId,
                    name,
                    null,
                    admin,
                    participants,
                    List.of(session),
                    activeOccurrence,
                    roundRepository.findByOccurrence(activeOccurrence));
            return Optional.of(restoredGroup);
        }
    }

    private static class InMemoryRoundRepository implements RoundRepository {

        private final List<Round> persistedRounds = new ArrayList<>();
        private final List<Round> saveCalls = new ArrayList<>();

        @Override
        public void create(Round round) {
            save(round);
        }

        @Override
        public void replace(Round expected, Round updated) {
            if (!persistedRounds.contains(expected)) {
                throw new IllegalStateException("Round was changed");
            }
            save(updated);
        }

        @Override
        public void save(Round round) {
            saveCalls.add(copy(round));
            int existingIndex = indexOf(round.occurrence(), round.number());
            if (existingIndex < 0) {
                persistedRounds.add(copy(round));
            } else {
                persistedRounds.set(existingIndex, copy(round));
            }
        }

        private void seed(Round round) {
            persistedRounds.add(copy(round));
        }

        private List<Round> findByOccurrence(SessionOccurrence occurrence) {
            return persistedRounds.stream()
                    .filter(round -> round.occurrence().equals(occurrence))
                    .map(round -> new Round(occurrence, round.number(), round.status(), round.combats()))
                    .toList();
        }

        private int indexOf(SessionOccurrence occurrence, int number) {
            for (int index = 0; index < persistedRounds.size(); index++) {
                Round round = persistedRounds.get(index);
                if (round.occurrence().equals(occurrence) && round.number() == number) {
                    return index;
                }
            }
            return -1;
        }

        private static Round copy(Round round) {
            return new Round(round.occurrence(), round.number(), round.status(), round.combats());
        }

        private List<Round> persistedRounds() {
            return List.copyOf(persistedRounds);
        }

        private List<Round> saveCalls() {
            return List.copyOf(saveCalls);
        }
    }
}

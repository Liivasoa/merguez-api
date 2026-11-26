package mg.msys.merguez_api.group.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.application.port.in.GetCurrentGroupSessionUseCase;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.exception.CurrentGroupSessionNotFoundException;
import mg.msys.merguez_api.group.application.result.CurrentGroupSessionResult;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

class GetCurrentGroupSessionServiceTest {

    @Test
    void returnsScheduledSessionCompletedRoundsAndCurrentRoundForTheRequestedGroup() {
        UserId admin = user();
        UserId firstMember = user();
        UserId secondMember = user();
        Group group = Group.create("Tuesday dojo", admin, List.of(firstMember, secondMember));
        Session scheduledSession = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, scheduledSession);
        Round firstRound = group.addRound(admin, occurrence, List.of(
                new Combat(List.of(admin, firstMember), Optional.of(admin))));
        Round completedRound = group.closeRound(admin, firstRound, firstRound.combats());
        Round currentRound = group.addRound(admin, occurrence, List.of(
                new Combat(List.of(admin, secondMember), Optional.of(admin))));
        AtomicReference<mg.msys.merguez_api.group.domain.model.GroupId> loadedGroupId = new AtomicReference<>();
        LoadGroupWithActiveOccurrencePort loader = groupId -> {
            loadedGroupId.set(groupId);
            return Optional.of(group);
        };
        GetCurrentGroupSessionUseCase useCase = new GetCurrentGroupSessionService(loader);

        CurrentGroupSessionResult result = useCase.execute(group.id());

        assertEquals(group.id(), loadedGroupId.get());
        assertSame(occurrence, result.occurrence());
        assertSame(scheduledSession, result.scheduledSession());
        assertEquals(List.of(completedRound), result.completedRounds());
        assertSame(currentRound, result.currentRound());
        assertEquals(RoundStatus.IN_PROGRESS, result.currentRound().status());
        assertEquals(currentRound.number(), result.currentRound().number());
        assertEquals(currentRound.combats(), result.currentRound().combats());
    }

    @Test
    void throwsCurrentGroupSessionNotFoundWhenLoaderReturnsEmpty() {
        GroupId groupId = GroupId.random();
        GetCurrentGroupSessionUseCase useCase = new GetCurrentGroupSessionService(groupIdToLoad -> Optional.empty());

        assertThrows(CurrentGroupSessionNotFoundException.class, () -> useCase.execute(groupId));
    }

    private static UserId user() {
        return new UserId(UUID.randomUUID());
    }
}

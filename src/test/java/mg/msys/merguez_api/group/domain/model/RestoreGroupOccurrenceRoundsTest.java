package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class RestoreGroupOccurrenceRoundsTest {

    @Test
    @DisplayName("A restored active occurrence retains its rounds and accepts further round activity")
    void restoredActiveOccurrenceRetainsRoundsAndCanContinue() {
        GroupId groupId = GroupId.random();
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId thirdMember = new UserId(UUID.randomUUID());
        UserId fourthMember = new UserId(UUID.randomUUID());
        List<UserId> participants = List.of(firstMember, secondMember, thirdMember, fourthMember);
        Session session = new Session(
                groupId, UUID.randomUUID(), "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = new SessionOccurrence(groupId, session.id(), Instant.now());
        Round firstRound = new Round(
                occurrence, 1, RoundStatus.COMPLETED,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));
        Round secondRound = new Round(
                occurrence, 2, RoundStatus.COMPLETED,
                List.of(new Combat(List.of(thirdMember, fourthMember), Optional.empty())));

        Group restoredGroup = Group.restore(
                groupId,
                "Tuesday dojo",
                null,
                admin,
                participants,
                List.of(session),
                occurrence,
                List.of(firstRound, secondRound));

        assertEquals(List.of(occurrence), restoredGroup.activeOccurrences());
        List<Round> restoredRounds = restoredGroup.rounds(occurrence);
        assertEquals(List.of(firstRound, secondRound), restoredRounds);
        assertThrows(UnsupportedOperationException.class, () -> restoredRounds.add(firstRound));

        Round nextRound = restoredGroup.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));
        assertEquals(3, nextRound.number());

        Combat reusingAssignedParticipant = new Combat(List.of(firstMember, thirdMember), Optional.empty());
        assertThrows(
                IllegalArgumentException.class,
                () -> restoredGroup.addCombat(firstMember, firstRound, reusingAssignedParticipant));
    }
}

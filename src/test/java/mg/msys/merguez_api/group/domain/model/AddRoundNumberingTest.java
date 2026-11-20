package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class AddRoundNumberingTest {

    @Test
    @DisplayName("A group member adds the next round after the previous round has finished")
    void groupMemberAddsNextRoundAfterPreviousRoundHasFinished() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId thirdMember = new UserId(UUID.randomUUID());
        UserId fourthMember = new UserId(UUID.randomUUID());
        Group group = Group.create(
                "Tuesday dojo", admin, List.of(firstMember, secondMember, thirdMember, fourthMember));
        Session session = group.createSession(
                firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(firstMember, session);

        Round firstRound = group.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));
        Round finishedRound = new Round(
                occurrence, firstRound.number(), RoundStatus.COMPLETED, firstRound.combats());
        Group restored = Group.restore(
                group.id(), group.name(), null, admin,
                List.of(firstMember, secondMember, thirdMember, fourthMember),
                List.of(session), occurrence, List.of(finishedRound));
        Round secondRound = restored.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(thirdMember, fourthMember), Optional.empty())));

        assertEquals(occurrence, firstRound.occurrence());
        assertEquals(occurrence, secondRound.occurrence());
        assertEquals(1, firstRound.number());
        assertEquals(2, secondRound.number());
        assertEquals(RoundStatus.IN_PROGRESS, firstRound.status());
        assertEquals(RoundStatus.IN_PROGRESS, secondRound.status());
    }

    @Test
    @DisplayName("A group cannot start another round while a round is in progress")
    void refusesAnotherRoundWhileOneIsInProgress() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(admin, session);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round firstRound = group.addRound(admin, occurrence, List.of(combat));

        assertThrows(IllegalStateException.class, () -> group.addRound(member, occurrence, List.of(combat)));
        assertEquals(RoundStatus.IN_PROGRESS, firstRound.status());
        assertEquals(List.of(firstRound), group.rounds(occurrence));
    }

    @Test
    @DisplayName("An in-progress round does not prevent another group from starting a round")
    void anotherGroupCanStartRound() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group firstGroup = Group.create("First", admin, List.of(member));
        Session firstSession = firstGroup.createSession(admin, "Practice", DayOfWeek.MONDAY, LocalTime.NOON);
        SessionOccurrence firstOccurrence = firstGroup.startSession(admin, firstSession);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round firstRound = firstGroup.addRound(admin, firstOccurrence, List.of(combat));
        Group secondGroup = Group.create("Second", admin, List.of(member));
        Session secondSession = secondGroup.createSession(admin, "Practice", DayOfWeek.MONDAY, LocalTime.NOON);
        SessionOccurrence secondOccurrence = secondGroup.startSession(admin, secondSession);

        Round secondRound = secondGroup.addRound(member, secondOccurrence, List.of(combat));

        assertEquals(RoundStatus.IN_PROGRESS, secondRound.status());
        assertEquals(List.of(firstRound), firstGroup.rounds(firstOccurrence));
    }
}

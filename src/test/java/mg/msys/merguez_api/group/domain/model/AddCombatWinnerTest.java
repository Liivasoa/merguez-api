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

class AddCombatWinnerTest {

    @Test
    @DisplayName("A combat winner must be one of its participants")
    void combatWinnerMustBeParticipant() {
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId unrelatedMember = new UserId(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () -> new Combat(List.of(firstMember, secondMember), Optional.of(unrelatedMember)));
    }

    @Test
    @DisplayName("A group member adds a combat with its chosen winner to a round")
    void groupMemberAddsCombatWithChosenWinnerToRound() {
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
        Combat initialCombat = new Combat(List.of(firstMember, secondMember), Optional.empty());
        Round round = group.addRound(firstMember, occurrence, List.of(initialCombat));
        Combat additionalCombat = new Combat(List.of(thirdMember, fourthMember), Optional.of(thirdMember));

        Round updatedRound = group.addCombat(thirdMember, round, additionalCombat);

        assertEquals(List.of(initialCombat, additionalCombat), updatedRound.combats());
        assertEquals(Optional.of(thirdMember), updatedRound.combats().get(1).winner());
    }
}

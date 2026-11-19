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

class AddCombatUniquenessTest {

    @Test
    @DisplayName("A round cannot assign a participant to more than one combat")
    void roundCannotContainOverlappingCombatParticipants() {
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId thirdMember = new UserId(UUID.randomUUID());
        GroupId groupId = GroupId.random();
        SessionOccurrence occurrence =
                new SessionOccurrence(groupId, UUID.randomUUID(), Instant.now());
        List<Combat> combats = List.of(
                new Combat(List.of(firstMember, secondMember), Optional.empty()),
                new Combat(List.of(firstMember, thirdMember), Optional.empty()));

        assertThrows(IllegalArgumentException.class, () -> new Round(occurrence, 1, combats));
    }

    @Test
    @DisplayName("A group member cannot add a combat with a participant already assigned in the round")
    void groupMemberCannotAddCombatWithParticipantAlreadyAssignedInRound() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId thirdMember = new UserId(UUID.randomUUID());
        Group group = Group.create(
                "Tuesday dojo", admin, List.of(firstMember, secondMember, thirdMember));
        Session session = group.createSession(
                firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(firstMember, session);
        Combat initialCombat = new Combat(List.of(firstMember, secondMember), Optional.empty());
        Round round = group.addRound(firstMember, occurrence, List.of(initialCombat));
        Combat combatWithAlreadyAssignedMember = new Combat(List.of(firstMember, thirdMember), Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> group.addCombat(firstMember, round, combatWithAlreadyAssignedMember));

        assertEquals(List.of(initialCombat), round.combats());
        assertEquals(List.of(firstMember, secondMember), round.combats().get(0).participants());
    }
}

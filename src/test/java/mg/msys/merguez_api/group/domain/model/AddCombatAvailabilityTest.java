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

class AddCombatAvailabilityTest {

    @Test
    @DisplayName("A combat cannot be added when fewer than two group members are unassigned")
    void cannotAddCombatWhenFewerThanTwoMembersAreUnassigned() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        UserId thirdMember = new UserId(UUID.randomUUID());
        Combat initialCombat = new Combat(List.of(firstMember, secondMember), Optional.empty());

        Group groupWithOneAvailable = Group.create(
                "Small dojo", admin, List.of(firstMember, secondMember));
        Session smallSession = groupWithOneAvailable.createSession(
                admin, "Training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence smallOccurrence = groupWithOneAvailable.startSession(admin, smallSession);
        Round smallRound = groupWithOneAvailable.addRound(admin, smallOccurrence, List.of(initialCombat));
        Combat combatWithOneAvailable = new Combat(List.of(admin, firstMember), Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> groupWithOneAvailable.addCombat(admin, smallRound, combatWithOneAvailable));
        assertEquals(List.of(initialCombat), smallRound.combats());

        Group groupWithNoneAvailable = Group.create(
                "Large dojo", admin, List.of(firstMember, secondMember, thirdMember));
        Session largeSession = groupWithNoneAvailable.createSession(
                admin, "Training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence largeOccurrence = groupWithNoneAvailable.startSession(admin, largeSession);
        Round largeRound = groupWithNoneAvailable.addRound(admin, largeOccurrence, List.of(initialCombat));
        Combat additionalCombat = new Combat(List.of(thirdMember, admin), Optional.empty());
        Round fullRound = groupWithNoneAvailable.addCombat(admin, largeRound, additionalCombat);
        Combat combatWithNoneAvailable = new Combat(List.of(firstMember, thirdMember), Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> groupWithNoneAvailable.addCombat(admin, fullRound, combatWithNoneAvailable));
        assertEquals(List.of(initialCombat, additionalCombat), fullRound.combats());
    }
}

package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class AddRoundToSessionTest {

    @Test
    @DisplayName("A group admin adds the first round with a combat without a winner")
    void groupAdminAddsFirstRoundWithCombatWithoutWinner() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(firstMember, secondMember));
        Session session = group.createSession(
                admin, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(admin, session);

        Round round = group.addRound(
                admin,
                occurrence,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));

        assertEquals(occurrence, round.occurrence());
        assertEquals(1, round.combats().size());
        Combat combat = round.combats().get(0);
        assertEquals(2, combat.participants().size());
        assertEquals(List.of(firstMember, secondMember), combat.participants());
        assertTrue(combat.winner().isEmpty());
    }

    @Test
    @DisplayName("A group member adds the first round with a combat without a winner")
    void groupMemberAddsFirstRoundWithCombatWithoutWinner() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(firstMember, secondMember));
        Session session = group.createSession(
                firstMember, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence occurrence = group.startSession(firstMember, session);

        Round round = group.addRound(
                firstMember,
                occurrence,
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty())));

        assertEquals(occurrence, round.occurrence());
        assertEquals(1, round.combats().size());
        Combat combat = round.combats().get(0);
        assertEquals(2, combat.participants().size());
        assertEquals(List.of(firstMember, secondMember), combat.participants());
        assertTrue(combat.winner().isEmpty());
    }

    @Test
    @DisplayName("A group admin cannot add a round to an occurrence that has not been started")
    void groupAdminCannotAddRoundToUnstartedSessionOccurrence() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId firstMember = new UserId(UUID.randomUUID());
        UserId secondMember = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(firstMember, secondMember));
        Session session = group.createSession(
                admin, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 0));
        SessionOccurrence unstartedOccurrence = new SessionOccurrence(group.id(), session.id(), Instant.now());
        List<Combat> validCombats =
                List.of(new Combat(List.of(firstMember, secondMember), Optional.empty()));

        assertTrue(group.activeOccurrences().isEmpty());
        assertThrows(
                IllegalStateException.class,
                () -> group.addRound(admin, unstartedOccurrence, validCombats));
        assertTrue(group.activeOccurrences().isEmpty());
    }
}

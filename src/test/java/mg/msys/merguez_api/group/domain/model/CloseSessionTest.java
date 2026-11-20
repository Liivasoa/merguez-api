package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.user.domain.model.UserId;

class CloseSessionTest {

    @Test
    void adminClosesSessionAndSettlesCombatResult() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round round = group.addRound(admin, occurrence, List.of(combat));
        group.closeRound(admin, round, List.of(new Combat(combat.participants(), Optional.of(admin))));

        var result = group.closeSession(admin, occurrence);

        assertTrue(result.occurrence().isClosed());
        assertEquals(Map.of(admin, 1, member, -1), result.netScores());
        assertEquals(1, result.payments().size());
        var payment = result.payments().getFirst();
        assertEquals(member, payment.debtor());
        assertEquals(admin, payment.creditor());
        assertEquals(1, payment.amount());
    }

    @Test
    void memberClosesSessionAndCalculatesCombatResult() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round round = group.addRound(admin, occurrence, List.of(combat));
        group.closeRound(admin, round, List.of(new Combat(combat.participants(), Optional.of(member))));
        assertTrue(group.isMember(member));
        assertFalse(group.isAdmin(member));
        assertFalse(occurrence.isClosed());
        assertEquals(RoundStatus.CLOSED, group.rounds(occurrence).getFirst().status());

        var result = group.closeSession(member, occurrence);

        assertTrue(occurrence.isClosed());
        assertEquals(occurrence, result.occurrence());
        assertEquals(Map.of(admin, -1, member, 1), result.netScores());
    }
}

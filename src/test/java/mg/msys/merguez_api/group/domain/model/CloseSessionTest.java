package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    void settlesNetScoresAcrossClosedRoundsIncludingInactiveMember() {
        UserId a = new UserId(UUID.randomUUID());
        UserId b = new UserId(UUID.randomUUID());
        UserId c = new UserId(UUID.randomUUID());
        UserId d = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", a, List.of(b, c, d));
        Session session = group.createSession(a, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(a, session);

        for (Combat result : List.of(
                new Combat(List.of(a, b), Optional.of(a)),
                new Combat(List.of(a, b), Optional.of(a)),
                new Combat(List.of(b, c), Optional.of(b)))) {
            Combat combat = new Combat(result.participants(), Optional.empty());
            Round round = group.addRound(a, occurrence, List.of(combat));
            group.closeRound(a, round, List.of(result));
        }
        assertEquals(3, group.rounds(occurrence).size());
        assertTrue(group.rounds(occurrence).stream().allMatch(round -> round.status() == RoundStatus.CLOSED));

        var result = group.closeSession(a, occurrence);

        assertTrue(result.occurrence().isClosed());
        assertEquals(Map.of(a, 2, b, -1, c, -1, d, 0), result.netScores());
        assertEquals(0, result.netScores().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, result.payments().size());
        assertEquals(Set.of(new SessionPayment(b, a, 1), new SessionPayment(c, a, 1)),
                Set.copyOf(result.payments()));

        Map<UserId, Integer> remaining = new HashMap<>(result.netScores());
        for (SessionPayment payment : result.payments()) {
            remaining.compute(payment.debtor(), (ignored, score) -> score + payment.amount());
            remaining.compute(payment.creditor(), (ignored, score) -> score - payment.amount());
        }
        assertEquals(Map.of(a, 0, b, 0, c, 0, d, 0), remaining);
    }

    @Test
    void createsNoPaymentsWhenCombatResultsCancelOutOrHaveNoWinner() {
        UserId a = new UserId(UUID.randomUUID());
        UserId b = new UserId(UUID.randomUUID());
        UserId c = new UserId(UUID.randomUUID());
        UserId d = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", a, List.of(b, c, d));
        Session session = group.createSession(a, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(a, session);

        Combat ab = new Combat(List.of(a, b), Optional.empty());
        Combat cd = new Combat(List.of(c, d), Optional.empty());
        Round firstRound = group.addRound(a, occurrence, List.of(ab, cd));
        group.closeRound(a, firstRound, List.of(new Combat(ab.participants(), Optional.of(a)), cd));
        Round secondRound = group.addRound(a, occurrence, List.of(ab));
        group.closeRound(a, secondRound, List.of(new Combat(ab.participants(), Optional.of(b))));
        assertEquals(2, group.rounds(occurrence).size());
        assertTrue(group.rounds(occurrence).stream().allMatch(round -> round.status() == RoundStatus.CLOSED));

        var result = group.closeSession(a, occurrence);

        assertTrue(result.occurrence().isClosed());
        assertEquals(Map.of(a, 0, b, 0, c, 0, d, 0), result.netScores());
        assertTrue(result.payments().isEmpty());
    }

    @Test
    void settlesMatchingPositionsInTwoPaymentsRegardlessOfPaymentOrder() {
        UserId a = new UserId(UUID.randomUUID());
        UserId b = new UserId(UUID.randomUUID());
        UserId c = new UserId(UUID.randomUUID());
        UserId d = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", a, List.of(b, c, d));
        Session session = group.createSession(a, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(a, session);

        Combat ad = new Combat(List.of(a, d), Optional.empty());
        Combat bc = new Combat(List.of(b, c), Optional.empty());
        for (int i = 0; i < 7; i++) {
            Round round = group.addRound(a, occurrence, List.of(ad, bc));
            group.closeRound(a, round, List.of(
                    new Combat(ad.participants(), Optional.of(a)),
                    new Combat(bc.participants(), Optional.of(b))));
        }
        Round lastRound = group.addRound(a, occurrence, List.of(ad));
        group.closeRound(a, lastRound, List.of(new Combat(ad.participants(), Optional.of(a))));
        assertEquals(8, group.rounds(occurrence).size());
        assertTrue(group.rounds(occurrence).stream().allMatch(round -> round.status() == RoundStatus.CLOSED));

        var result = group.closeSession(a, occurrence);

        assertTrue(result.occurrence().isClosed());
        assertEquals(Map.of(a, 8, b, 7, c, -7, d, -8), result.netScores());
        assertEquals(0, result.netScores().values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(2, result.payments().size());
        assertEquals(Set.of(new SessionPayment(d, a, 8), new SessionPayment(c, b, 7)),
                Set.copyOf(result.payments()));

        Map<UserId, Integer> remaining = new HashMap<>(result.netScores());
        for (SessionPayment payment : result.payments()) {
            remaining.compute(payment.debtor(), (ignored, score) -> score + payment.amount());
            remaining.compute(payment.creditor(), (ignored, score) -> score - payment.amount());
        }
        assertEquals(Map.of(a, 0, b, 0, c, 0, d, 0), remaining);
        assertEquals(0, remaining.values().stream().mapToInt(Integer::intValue).sum());
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

    @Test
    void refusesToCloseAnAlreadyClosedSessionWithoutChangingState() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        Session nextSession = group.createSession(admin, "Later practice", DayOfWeek.TUESDAY, LocalTime.of(13, 0));
        SessionOccurrence occurrence = group.startSession(admin, session);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round round = group.addRound(admin, occurrence, List.of(combat));
        Round closedRound = group.closeRound(admin, round,
                List.of(new Combat(combat.participants(), Optional.of(admin))));
        var firstResult = group.closeSession(admin, occurrence);
        SessionOccurrence activeOccurrence = group.startSession(admin, nextSession);
        var activeOccurrences = group.activeOccurrences();
        var sessions = group.sessions();
        var members = group.members();
        var scores = firstResult.netScores();
        var payments = firstResult.payments();
        assertTrue(occurrence.isClosed());
        assertFalse(activeOccurrence.isClosed());
        assertEquals(List.of(closedRound), group.rounds(occurrence));
        assertEquals(Map.of(admin, 1, member, -1), scores);
        assertEquals(1, payments.size());

        assertThrows(IllegalStateException.class, () -> group.closeSession(member, occurrence));

        assertTrue(occurrence.isClosed());
        assertFalse(activeOccurrence.isClosed());
        assertEquals(activeOccurrences, group.activeOccurrences());
        assertEquals(sessions, group.sessions());
        assertEquals(members, group.members());
        assertEquals(List.of(closedRound), group.rounds(occurrence));
        assertTrue(group.rounds(activeOccurrence).isEmpty());
        assertEquals(scores, firstResult.netScores());
        assertEquals(payments, firstResult.payments());
    }

    @Test
    void refusesToCloseAnActiveSessionWithoutRoundsWithoutChangingState() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        var activeOccurrences = group.activeOccurrences();
        var sessions = group.sessions();
        var members = group.members();
        assertEquals(List.of(occurrence), activeOccurrences);
        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());
        assertTrue(group.rounds(occurrence).isEmpty());

        assertThrows(IllegalStateException.class, () -> group.closeSession(admin, occurrence));

        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());
        assertEquals(activeOccurrences, group.activeOccurrences());
        assertEquals(sessions, group.sessions());
        assertEquals(members, group.members());
        assertTrue(group.rounds(occurrence).isEmpty());
    }

    @Test
    void refusesToCloseSessionWithAnUnclosedRoundWithoutChangingState() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        Combat combat = new Combat(List.of(admin, member), Optional.empty());
        Round firstRound = group.addRound(admin, occurrence, List.of(combat));
        Combat result = new Combat(combat.participants(), Optional.of(admin));
        Round closedRound = group.closeRound(admin, firstRound, List.of(result));
        Round ongoingRound = group.addRound(admin, occurrence, List.of(combat));
        var activeOccurrences = group.activeOccurrences();
        var sessions = group.sessions();
        var members = group.members();
        var rounds = group.rounds(occurrence);
        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());
        assertEquals(List.of(occurrence), activeOccurrences);
        assertEquals(List.of(closedRound, ongoingRound), rounds);
        assertEquals(RoundStatus.CLOSED, closedRound.status());
        assertEquals(List.of(result), closedRound.combats());
        assertEquals(RoundStatus.IN_PROGRESS, ongoingRound.status());
        assertEquals(List.of(combat), ongoingRound.combats());

        assertThrows(IllegalStateException.class, () -> group.closeSession(admin, occurrence));

        assertTrue(occurrence.isStarted());
        assertFalse(occurrence.isClosed());
        assertEquals(activeOccurrences, group.activeOccurrences());
        assertEquals(sessions, group.sessions());
        assertEquals(members, group.members());
        assertEquals(rounds, group.rounds(occurrence));
    }
}

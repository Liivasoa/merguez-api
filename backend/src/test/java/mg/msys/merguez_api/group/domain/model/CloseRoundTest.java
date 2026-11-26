package mg.msys.merguez_api.group.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.exception.InvalidRoundResultsException;
import mg.msys.merguez_api.group.domain.exception.RoundClosedException;
import mg.msys.merguez_api.group.domain.exception.RoundDoesNotBelongToOccurrenceException;
import mg.msys.merguez_api.group.domain.exception.RoundNotInProgressException;
import mg.msys.merguez_api.user.domain.model.UserId;

class CloseRoundTest {

    @Test
    void closesWithOptionalWinnersAndAllowsNextRound() {
        Fixture f = new Fixture();
        Combat firstResult = new Combat(List.of(f.first, f.admin), Optional.of(f.first));
        Combat secondResult = new Combat(List.of(f.second, f.third), Optional.empty());

        Round closed = f.group.closeRound(f.first, f.round, List.of(secondResult, firstResult));

        assertEquals(RoundStatus.CLOSED, closed.status());
        assertEquals(List.of(
                new Combat(List.of(f.admin, f.first), Optional.of(f.first)),
                secondResult), closed.combats());
        assertEquals(List.of(closed), f.group.rounds(f.occurrence));
        assertEquals(2, f.group.addRound(f.admin, f.occurrence, List.of(firstResult)).number());
    }

    @Test
    void rejectsIncorrectResultsWithoutChangingTheRound() {
        Fixture f = new Fixture();
        assertThrows(InvalidRoundResultsException.class,
                () -> f.group.closeRound(f.admin, f.round, List.of(f.round.combats().getFirst())));
        assertThrows(InvalidRoundResultsException.class,
                () -> f.group.closeRound(f.admin, f.round, List.of(
                        new Combat(List.of(f.admin, f.second), Optional.empty()),
                        new Combat(List.of(f.first, f.third), Optional.empty()))));
        assertThrows(GroupAccessDeniedException.class,
                () -> f.group.closeRound(user(), f.round, f.round.combats()));
        assertEquals(List.of(f.round), f.group.rounds(f.occurrence));
    }

    @Test
    void refusesFurtherChangesToClosedRounds() {
        Fixture f = new Fixture();
        Round closed = f.group.closeRound(f.admin, f.round, f.round.combats());
        assertThrows(RoundNotInProgressException.class,
                () -> f.group.closeRound(f.admin, closed, closed.combats()));
        assertThrows(RoundClosedException.class,
                () -> f.group.addCombat(f.admin, closed,
                        new Combat(List.of(f.admin, f.first), Optional.empty())));
    }

    @Test
    void rejectsClosingAnOutdatedRoundSnapshot() {
        Fixture f = new Fixture();
        Round closed = f.group.closeRound(f.admin, f.round, f.round.combats());

        assertThrows(RoundDoesNotBelongToOccurrenceException.class,
                () -> f.group.closeRound(f.admin, f.round, f.round.combats()));
        assertEquals(List.of(closed), f.group.rounds(f.occurrence));
    }

    private static UserId user() {
        return new UserId(UUID.randomUUID());
    }

    private static class Fixture {
        final UserId admin = user();
        final UserId first = user();
        final UserId second = user();
        final UserId third = user();
        final Group group = Group.create("Training", admin, List.of(first, second, third));
        final Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        final SessionOccurrence occurrence = group.startSession(admin, session);
        final Round round = group.addRound(admin, occurrence, List.of(
                new Combat(List.of(admin, first), Optional.of(admin)),
                new Combat(List.of(second, third), Optional.empty())));
    }
}

package mg.msys.merguez_api.group.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

import mg.msys.merguez_api.group.domain.exception.SessionClosureRejectedException;
import mg.msys.merguez_api.group.domain.service.SessionSettlementCalculator;
import mg.msys.merguez_api.user.domain.model.UserId;

public final class SessionOccurrence {

    private final GroupId groupId;
    private final UUID scheduleId;
    private final Instant startedAt;
    private final List<Round> rounds = new ArrayList<>();
    private boolean roundsAttached;
    private boolean closed;

    public SessionOccurrence(GroupId groupId, UUID scheduleId, Instant startedAt) {
        this.groupId = groupId;
        this.scheduleId = scheduleId;
        this.startedAt = startedAt;
    }

    public GroupId groupId() {
        return groupId;
    }

    public UUID scheduleId() {
        return scheduleId;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public boolean isStarted() {
        return true;
    }

    public boolean isClosed() {
        return closed;
    }

    SessionOccurrence attachRounds(List<Round> restoredRounds) {
        for (int index = 0; index < restoredRounds.size(); index++) {
            Round round = restoredRounds.get(index);
            if (round == null || !equals(round.occurrence())) {
                throw new IllegalArgumentException("Restored rounds must belong to the active occurrence");
            }
            if (round.number() != index + 1) {
                throw new IllegalArgumentException("Restored rounds must be numbered consecutively from one");
            }
        }
        SessionOccurrence occurrence = this;
        if (roundsAttached) {
            // Rehydrating a second group snapshot must not overwrite the first one's rounds.
            occurrence = new SessionOccurrence(groupId, scheduleId, startedAt);
            occurrence.closed = closed;
        }
        for (Round round : restoredRounds) {
            occurrence.rounds.add(round.occurrence() == occurrence
                    ? round
                    : new Round(occurrence, round.number(), round.status(), round.combats()));
        }
        occurrence.roundsAttached = true;
        return occurrence;
    }

    List<Round> rounds() {
        return List.copyOf(rounds);
    }

    Round addRound(List<Combat> combats, Consumer<Combat> requireGroupParticipants) {
        if (rounds.stream().anyMatch(round -> round.status() == RoundStatus.IN_PROGRESS)) {
            throw new IllegalStateException("A round is already in progress in this group");
        }
        if (combats == null || combats.isEmpty()) {
            throw new IllegalArgumentException("A round must contain at least one combat");
        }
        combats.forEach(requireGroupParticipants);
        Round round = new Round(this, rounds.size() + 1, combats);
        rounds.add(round);
        return round;
    }

    Round addCombat(Round expected, Combat combat, Consumer<Combat> requireGroupParticipants) {
        int index = requireRoundIndex(expected);
        Round updated = rounds.get(index).addCombat(combat, requireGroupParticipants);
        rounds.set(index, updated);
        return updated;
    }

    Round closeRound(Round expected, List<Combat> results) {
        int index = requireRoundIndex(expected);
        Round closedRound = rounds.get(index).close(results);
        rounds.set(index, closedRound);
        return closedRound;
    }

    private int requireRoundIndex(Round expected) {
        int index = rounds.indexOf(expected);
        if (index < 0) {
            throw new IllegalArgumentException("Round must belong to this group");
        }
        return index;
    }

    SessionSettlement settleAndClose(List<UserId> memberIds) {
        if (rounds.isEmpty()) {
            throw new SessionClosureRejectedException("A session must contain at least one round");
        }
        if (rounds.stream().anyMatch(round -> round.status() != RoundStatus.CLOSED)) {
            throw new SessionClosureRejectedException("All rounds must be closed before closing the session");
        }
        SessionSettlement settlement = SessionSettlementCalculator.calculate(memberIds, rounds);
        markClosed();
        return settlement;
    }

    void markClosed() {
        closed = true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SessionOccurrence occurrence
                && Objects.equals(groupId, occurrence.groupId)
                && Objects.equals(scheduleId, occurrence.scheduleId)
                && Objects.equals(startedAt, occurrence.startedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, scheduleId, startedAt);
    }

    @Override
    public String toString() {
        return "SessionOccurrence[groupId=" + groupId + ", scheduleId=" + scheduleId + ", startedAt=" + startedAt + "]";
    }
}

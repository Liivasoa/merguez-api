package mg.msys.merguez_api.group.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class SessionOccurrence {

    private final GroupId groupId;
    private final UUID scheduleId;
    private final Instant startedAt;
    private final String persistenceToken;
    private List<Round> restoredRounds;
    private List<Round> closedRounds;
    private boolean closed;

    public SessionOccurrence(GroupId groupId, UUID scheduleId, Instant startedAt) {
        this(groupId, scheduleId, startedAt, null);
    }

    public SessionOccurrence(GroupId groupId, UUID scheduleId, Instant startedAt, String persistenceToken) {
        this.groupId = groupId;
        this.scheduleId = scheduleId;
        this.startedAt = startedAt;
        this.persistenceToken = persistenceToken;
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

    void restoreRounds(List<Round> rounds) {
        restoredRounds = List.copyOf(rounds);
    }

    void close(List<Round> rounds) {
        closedRounds = List.copyOf(rounds);
        closed = true;
    }

    public Optional<String> persistenceToken() {
        return Optional.ofNullable(persistenceToken);
    }

    public boolean closedWithRestoredRounds() {
        return closed && restoredRounds != null && restoredRounds.equals(closedRounds);
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

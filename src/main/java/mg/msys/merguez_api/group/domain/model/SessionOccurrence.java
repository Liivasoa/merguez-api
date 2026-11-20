package mg.msys.merguez_api.group.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class SessionOccurrence {

    private final GroupId groupId;
    private final UUID scheduleId;
    private final Instant startedAt;
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

    void close() {
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

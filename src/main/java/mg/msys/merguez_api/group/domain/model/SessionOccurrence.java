package mg.msys.merguez_api.group.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SessionOccurrence(GroupId groupId, UUID scheduleId, Instant startedAt) {

    public boolean isStarted() {
        return true;
    }
}

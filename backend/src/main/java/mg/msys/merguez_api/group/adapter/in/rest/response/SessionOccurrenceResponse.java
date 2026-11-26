package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.time.Instant;
import java.util.UUID;

import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public record SessionOccurrenceResponse(UUID groupId, UUID scheduleId, Instant startedAt) {

    public static SessionOccurrenceResponse from(SessionOccurrence occurrence) {
        return new SessionOccurrenceResponse(
                occurrence.groupId().value(),
                occurrence.scheduleId(),
                occurrence.startedAt());
    }
}

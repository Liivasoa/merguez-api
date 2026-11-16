package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import mg.msys.merguez_api.group.domain.model.Session;

public record SessionResponse(UUID groupId, String name, DayOfWeek day, LocalTime startTime) {

    public static SessionResponse from(Session session) {
        return new SessionResponse(session.groupId().value(), session.name(), session.day(), session.startTime());
    }
}

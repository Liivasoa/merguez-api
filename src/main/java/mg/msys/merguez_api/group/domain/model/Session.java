package mg.msys.merguez_api.group.domain.model;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record Session(GroupId groupId, String name, DayOfWeek day, LocalTime startTime) {

    public UUID id() {
        String identity = groupId.value() + ":" + day + ":" + startTime;
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }
}

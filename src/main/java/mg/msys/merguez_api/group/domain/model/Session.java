package mg.msys.merguez_api.group.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record Session(GroupId groupId, String name, DayOfWeek day, LocalTime startTime) {
}

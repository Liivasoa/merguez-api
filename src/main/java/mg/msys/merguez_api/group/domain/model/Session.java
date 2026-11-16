package mg.msys.merguez_api.group.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record Session(GroupId groupId, UUID id, String name, DayOfWeek day, LocalTime startTime) {}

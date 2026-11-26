package mg.msys.merguez_api.group.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CreateSessionCommand(
        GroupId groupId,
        UserId requester,
        String name,
        DayOfWeek day,
        LocalTime startTime) {
}

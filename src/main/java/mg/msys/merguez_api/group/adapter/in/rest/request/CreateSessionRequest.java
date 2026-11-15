package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import mg.msys.merguez_api.group.application.command.CreateSessionCommand;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CreateSessionRequest(
        @NotNull UUID requesterId,
        String name,
        @NotNull DayOfWeek day,
        @NotNull LocalTime startTime) {

    public CreateSessionCommand toCommand(GroupId groupId) {
        return new CreateSessionCommand(
                groupId,
                new UserId(requesterId),
                name,
                day,
                startTime);
    }
}

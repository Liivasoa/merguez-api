package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record StartSessionRequest(@NotNull UUID requesterId) {

    public StartSessionCommand toCommand(GroupId groupId, UUID scheduledSessionId) {
        return new StartSessionCommand(groupId, new UserId(requesterId), scheduledSessionId);
    }
}

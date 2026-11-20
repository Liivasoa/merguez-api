package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CloseSessionRequest(@NotNull UUID requesterId) {

    public CloseSessionCommand toCommand(GroupId groupId, UUID scheduledSessionId) {
        return new CloseSessionCommand(groupId, new UserId(requesterId), scheduledSessionId);
    }
}

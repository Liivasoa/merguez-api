package mg.msys.merguez_api.group.application.command;

import java.util.UUID;

import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CloseSessionCommand(GroupId groupId, UserId requester, UUID scheduledSessionId) {
}

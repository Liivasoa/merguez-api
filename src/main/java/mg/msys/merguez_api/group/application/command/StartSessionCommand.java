package mg.msys.merguez_api.group.application.command;

import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.user.domain.model.UserId;

public record StartSessionCommand(
        GroupId groupId,
        UserId requester,
        Session scheduledSession) {
}

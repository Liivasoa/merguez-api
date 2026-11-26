package mg.msys.merguez_api.group.application.command;

import java.util.List;

import mg.msys.merguez_api.user.domain.model.UserId;

public record CreateGroupCommand(UserId creatorId, String name, String description, List<UserId> participantIds) {
}

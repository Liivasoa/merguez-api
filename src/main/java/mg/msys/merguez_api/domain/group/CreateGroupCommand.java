package mg.msys.merguez_api.domain.group;

import java.util.List;

import mg.msys.merguez_api.domain.user.UserId;

public record CreateGroupCommand(UserId creatorId, String name, String description, List<UserId> participantIds) {
}

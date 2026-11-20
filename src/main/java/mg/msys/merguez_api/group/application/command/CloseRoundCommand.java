package mg.msys.merguez_api.group.application.command;

import java.util.List;

import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CloseRoundCommand(GroupId groupId, UserId requester, int roundNumber, List<Combat> combats) {
}

package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateRoundRequest.CombatRequest;
import mg.msys.merguez_api.group.application.command.CloseRoundCommand;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CloseRoundRequest(
        @NotNull UUID requesterId,
        @NotEmpty List<@NotNull @Valid CombatRequest> combats) {

    public CloseRoundCommand toCommand(GroupId groupId, int roundNumber) {
        return new CloseRoundCommand(groupId, new UserId(requesterId), roundNumber,
                combats.stream().map(CombatRequest::toCombat).toList());
    }
}

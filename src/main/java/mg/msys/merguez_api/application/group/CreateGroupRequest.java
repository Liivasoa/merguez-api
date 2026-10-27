package mg.msys.merguez_api.application.group;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import mg.msys.merguez_api.domain.group.CreateGroupCommand;
import mg.msys.merguez_api.domain.user.UserId;

public record CreateGroupRequest(UUID creatorId, @NotBlank String name, String description, List<UUID> participantIds) {

    public CreateGroupCommand toCommand() {
        return new CreateGroupCommand(
                new UserId(creatorId),
                name,
                description,
                participantIds.stream().map(UserId::new).toList());
    }
}
package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import mg.msys.merguez_api.group.application.command.CreateGroupCommand;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CreateGroupRequest(@NotNull UUID creatorId, @NotBlank String name, String description,
        @NotEmpty List<UUID> participantIds) {

    @JsonIgnore
    @AssertTrue(message = "must include a participant other than the creator")
    public boolean isParticipantOtherThanCreatorPresent() {
        if (participantIds == null || participantIds.isEmpty()) {
            return true; // reported by @NotEmpty
        }
        return participantIds.stream().anyMatch(id -> !id.equals(creatorId));
    }

    public CreateGroupCommand toCommand() {
        return new CreateGroupCommand(
                new UserId(creatorId),
                name,
                description,
                participantIds.stream().map(UserId::new).toList());
    }
}
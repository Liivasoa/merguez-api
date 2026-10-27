package mg.msys.merguez_api.application.group;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import mg.msys.merguez_api.domain.group.CreateGroupCommand;
import mg.msys.merguez_api.domain.user.UserId;

public record CreateGroupRequest(UUID creatorId, @NotBlank String name, String description,
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
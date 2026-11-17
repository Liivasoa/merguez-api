package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mg.msys.merguez_api.group.application.command.AddCombatCommand;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record AddCombatRequest(
        @NotNull UUID requesterId,
        @NotNull @Size(min = 2, max = 2) List<@NotNull UUID> participantIds,
        UUID winnerId) {

    @JsonIgnore
    @AssertTrue
    boolean isParticipantsDistinct() {
        return participantIds == null || participantIds.stream().distinct().count() == participantIds.size();
    }

    public AddCombatCommand toCommand(GroupId groupId, int roundNumber) {
        return new AddCombatCommand(
                groupId,
                new UserId(requesterId),
                roundNumber,
                new Combat(
                        participantIds.stream().map(UserId::new).toList(),
                        Optional.ofNullable(winnerId).map(UserId::new)));
    }
}

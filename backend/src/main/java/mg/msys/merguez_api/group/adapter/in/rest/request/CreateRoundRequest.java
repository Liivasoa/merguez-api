package mg.msys.merguez_api.group.adapter.in.rest.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mg.msys.merguez_api.group.application.command.AddRoundCommand;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.user.domain.model.UserId;

public record CreateRoundRequest(
        @NotNull UUID requesterId,
        @NotEmpty List<@NotNull @Valid CombatRequest> combats) {

    public AddRoundCommand toCommand(GroupId groupId) {
        return new AddRoundCommand(
                groupId,
                new UserId(requesterId),
                combats.stream().map(CombatRequest::toCombat).toList());
    }

    public record CombatRequest(
            @NotNull @Size(min = 2, max = 2) List<@NotNull UUID> participantIds,
            UUID winnerId) {

        @JsonIgnore
        @AssertTrue
        boolean isParticipantsDistinct() {
            return participantIds == null || participantIds.stream().distinct().count() == participantIds.size();
        }

        @JsonIgnore
        @AssertTrue
        boolean isWinnerParticipant() {
            return winnerId == null || participantIds == null || participantIds.contains(winnerId);
        }

        public Combat toCombat() {
            return new Combat(
                    participantIds.stream().map(UserId::new).toList(),
                    Optional.ofNullable(winnerId).map(UserId::new));
        }
    }
}

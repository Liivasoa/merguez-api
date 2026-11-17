package mg.msys.merguez_api.group.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import mg.msys.merguez_api.user.domain.model.UserId;

public record Combat(List<UserId> participants, Optional<UserId> winner) {

    public Combat {
        participants = List.copyOf(Objects.requireNonNull(participants, "Combat participants are required"));
        winner = Objects.requireNonNull(winner, "Combat winner is required");
        if (participants.size() != 2 || participants.get(0).equals(participants.get(1))) {
            throw new IllegalArgumentException("A combat must have exactly two distinct participants");
        }
    }
}

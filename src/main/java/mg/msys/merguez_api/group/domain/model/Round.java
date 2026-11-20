package mg.msys.merguez_api.group.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import mg.msys.merguez_api.user.domain.model.UserId;

public record Round(SessionOccurrence occurrence, int number, RoundStatus status, List<Combat> combats) {

    public Round {
        occurrence = Objects.requireNonNull(occurrence, "Session occurrence is required");
        status = Objects.requireNonNull(status, "Round status is required");
        if (number < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
        combats = List.copyOf(Objects.requireNonNull(combats, "Round combats are required"));
        if (combats.isEmpty()) {
            throw new IllegalArgumentException("A round must contain at least one combat");
        }
        Set<UserId> assignedParticipants = new HashSet<>();
        for (Combat combat : combats) {
            if (combat.participants().stream().anyMatch(participant -> !assignedParticipants.add(participant))) {
                throw new IllegalArgumentException("A participant can appear in only one combat per round");
            }
        }
    }

    public Round(SessionOccurrence occurrence, int number, List<Combat> combats) {
        this(occurrence, number, RoundStatus.IN_PROGRESS, combats);
    }

    public Round(SessionOccurrence occurrence, List<Combat> combats) {
        this(occurrence, 1, combats);
    }
}

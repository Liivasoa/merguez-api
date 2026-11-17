package mg.msys.merguez_api.group.domain.model;

import java.util.List;
import java.util.Objects;

public record Round(SessionOccurrence occurrence, int number, List<Combat> combats) {

    public Round {
        occurrence = Objects.requireNonNull(occurrence, "Session occurrence is required");
        if (number < 1) {
            throw new IllegalArgumentException("Round number must be positive");
        }
        combats = List.copyOf(Objects.requireNonNull(combats, "Round combats are required"));
        if (combats.isEmpty()) {
            throw new IllegalArgumentException("A round must contain at least one combat");
        }
    }

    public Round(SessionOccurrence occurrence, List<Combat> combats) {
        this(occurrence, 1, combats);
    }
}

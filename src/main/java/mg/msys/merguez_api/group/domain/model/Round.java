package mg.msys.merguez_api.group.domain.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import mg.msys.merguez_api.group.domain.exception.InvalidRoundException;
import mg.msys.merguez_api.group.domain.exception.InvalidRoundResultsException;
import mg.msys.merguez_api.group.domain.exception.RoundClosedException;
import mg.msys.merguez_api.group.domain.exception.RoundNotInProgressException;
import mg.msys.merguez_api.user.domain.model.UserId;

public record Round(SessionOccurrence occurrence, int number, RoundStatus status, List<Combat> combats) {

    public Round {
        occurrence = Objects.requireNonNull(occurrence, "Session occurrence is required");
        status = Objects.requireNonNull(status, "Round status is required");
        if (number < 1) {
            throw new InvalidRoundException("Round number must be positive");
        }
        combats = List.copyOf(Objects.requireNonNull(combats, "Round combats are required"));
        if (combats.isEmpty()) {
            throw new InvalidRoundException("A round must contain at least one combat");
        }
        Set<UserId> assignedParticipants = new HashSet<>();
        for (Combat combat : combats) {
            if (combat.participants().stream().anyMatch(participant -> !assignedParticipants.add(participant))) {
                throw new InvalidRoundException("A participant can only appear in one combat per round");
            }
        }
    }

    public Round(SessionOccurrence occurrence, int number, List<Combat> combats) {
        this(occurrence, number, RoundStatus.IN_PROGRESS, combats);
    }

    public Round(SessionOccurrence occurrence, List<Combat> combats) {
        this(occurrence, 1, combats);
    }

    Round addCombat(Combat combat, Consumer<Combat> requireGroupParticipants) {
        if (status == RoundStatus.CLOSED) {
            throw new RoundClosedException();
        }
        requireGroupParticipants.accept(combat);
        List<Combat> updatedCombats = new ArrayList<>(combats);
        updatedCombats.add(combat);
        return new Round(occurrence, number, status, updatedCombats);
    }

    Round close(List<Combat> results) {
        if (status != RoundStatus.IN_PROGRESS) {
            throw new RoundNotInProgressException();
        }
        if (results == null || results.size() != combats.size() || results.stream().anyMatch(Objects::isNull)) {
            throw new InvalidRoundResultsException("Results must match all round combats");
        }
        List<Combat> updatedCombats = new ArrayList<>();
        for (Combat existing : combats) {
            List<Combat> matches = results.stream()
                    .filter(result -> result.participants().containsAll(existing.participants()))
                    .toList();
            if (matches.size() != 1) {
                throw new InvalidRoundResultsException("Results must match all round combats");
            }
            updatedCombats.add(new Combat(existing.participants(), matches.getFirst().winner()));
        }
        return new Round(occurrence, number, RoundStatus.CLOSED, updatedCombats);
    }
}

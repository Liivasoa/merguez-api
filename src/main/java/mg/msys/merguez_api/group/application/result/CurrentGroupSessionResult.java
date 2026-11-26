package mg.msys.merguez_api.group.application.result;

import java.util.List;
import java.util.Objects;

import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public record CurrentGroupSessionResult(
        SessionOccurrence occurrence,
        Session scheduledSession,
        List<Round> completedRounds,
        Round currentRound) {

    public CurrentGroupSessionResult {
        Objects.requireNonNull(occurrence);
        Objects.requireNonNull(scheduledSession);
        completedRounds = List.copyOf(completedRounds);
        Objects.requireNonNull(currentRound);
    }
}

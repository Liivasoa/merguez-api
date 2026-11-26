package mg.msys.merguez_api.group.application.usecase;

import java.util.List;
import java.util.Objects;

import mg.msys.merguez_api.group.application.exception.CurrentGroupSessionNotFoundException;
import mg.msys.merguez_api.group.application.port.in.GetCurrentGroupSessionUseCase;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.result.CurrentGroupSessionResult;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public final class GetCurrentGroupSessionService implements GetCurrentGroupSessionUseCase {

    private final LoadGroupWithActiveOccurrencePort groupLoader;

    public GetCurrentGroupSessionService(LoadGroupWithActiveOccurrencePort groupLoader) {
        this.groupLoader = Objects.requireNonNull(groupLoader);
    }

    @Override
    public CurrentGroupSessionResult execute(GroupId groupId) {
        Group group = groupLoader.loadByGroupId(groupId)
                .orElseThrow(() -> new CurrentGroupSessionNotFoundException(groupId));
        SessionOccurrence occurrence = group.activeOccurrences().getFirst();
        Session scheduledSession = group.sessions().stream()
                .filter(session -> session.id().equals(occurrence.scheduleId()))
                .findFirst()
                .orElseThrow();
        List<Round> rounds = group.rounds(occurrence);
        List<Round> completedRounds = rounds.stream()
                .filter(round -> round.status() != RoundStatus.IN_PROGRESS)
                .toList();
        Round currentRound = rounds.stream()
                .filter(round -> round.status() == RoundStatus.IN_PROGRESS)
                .findFirst()
                .orElseThrow();

        return new CurrentGroupSessionResult(occurrence, scheduledSession, completedRounds, currentRound);
    }
}

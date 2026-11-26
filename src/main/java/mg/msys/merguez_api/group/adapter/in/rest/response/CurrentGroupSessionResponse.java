package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.util.List;

import mg.msys.merguez_api.group.application.result.CurrentGroupSessionResult;

public record CurrentGroupSessionResponse(
        SessionOccurrenceResponse occurrence,
        SessionResponse scheduledSession,
        List<RoundResponse> completedRounds,
        RoundResponse currentRound) {

    public CurrentGroupSessionResponse {
        completedRounds = List.copyOf(completedRounds);
    }

    public static CurrentGroupSessionResponse from(CurrentGroupSessionResult result) {
        return new CurrentGroupSessionResponse(
                SessionOccurrenceResponse.from(result.occurrence()),
                SessionResponse.from(result.scheduledSession()),
                result.completedRounds().stream().map(RoundResponse::from).toList(),
                RoundResponse.from(result.currentRound()));
    }
}

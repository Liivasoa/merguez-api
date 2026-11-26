package mg.msys.merguez_api.group.domain.model;

import java.util.List;
import java.util.Map;

import mg.msys.merguez_api.user.domain.model.UserId;

public record SessionClosureResult(
        SessionOccurrence occurrence,
        Map<UserId, Integer> netScores,
        List<SessionPayment> payments) {

    public SessionClosureResult {
        netScores = Map.copyOf(netScores);
        payments = List.copyOf(payments);
    }
}

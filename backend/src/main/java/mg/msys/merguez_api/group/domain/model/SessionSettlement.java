package mg.msys.merguez_api.group.domain.model;

import java.util.List;
import java.util.Map;

import mg.msys.merguez_api.user.domain.model.UserId;

public record SessionSettlement(Map<UserId, Integer> netScores, List<SessionPayment> payments) {
    public SessionSettlement {
        netScores = Map.copyOf(netScores);
        payments = List.copyOf(payments);
    }
}

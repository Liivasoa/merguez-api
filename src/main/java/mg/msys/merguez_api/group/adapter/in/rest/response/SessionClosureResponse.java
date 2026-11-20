package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionPayment;

public record SessionClosureResponse(
        SessionOccurrenceResponse occurrence,
        Map<UUID, Integer> netScores,
        List<PaymentResponse> payments) {

    public static SessionClosureResponse from(SessionClosureResult result) {
        return new SessionClosureResponse(
                SessionOccurrenceResponse.from(result.occurrence()),
                result.netScores().entrySet().stream()
                        .collect(Collectors.toMap(entry -> entry.getKey().value(), Map.Entry::getValue)),
                result.payments().stream().map(PaymentResponse::from).toList());
    }

    public record PaymentResponse(UUID debtorId, UUID creditorId, int amount) {
        private static PaymentResponse from(SessionPayment payment) {
            return new PaymentResponse(payment.debtor().value(), payment.creditor().value(), payment.amount());
        }
    }
}

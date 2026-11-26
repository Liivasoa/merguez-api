package mg.msys.merguez_api.group.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionPayment;
import mg.msys.merguez_api.group.domain.model.SessionSettlement;
import mg.msys.merguez_api.user.domain.model.UserId;

public final class SessionSettlementCalculator {

    private SessionSettlementCalculator() {
    }

    public static SessionSettlement calculate(List<UserId> memberIds, List<Round> closedRounds) {
        Map<UserId, Integer> netScores = calculateNetScores(memberIds, closedRounds);
        return new SessionSettlement(netScores, calculatePayments(netScores));
    }

    private static Map<UserId, Integer> calculateNetScores(List<UserId> memberIds, List<Round> closedRounds) {
        Map<UserId, Integer> netScores = new LinkedHashMap<>();
        memberIds.forEach(memberId -> netScores.put(memberId, 0));
        closedRounds.stream()
                .flatMap(round -> round.combats().stream())
                .forEach(combat -> combat.winner().ifPresent(winner -> {
                    UserId loser = combat.participants().stream()
                            .filter(participant -> !participant.equals(winner))
                            .findFirst()
                            .orElseThrow();
                    netScores.compute(winner, (ignored, score) -> score + 1);
                    netScores.compute(loser, (ignored, score) -> score - 1);
                }));
        return netScores;
    }

    private static List<SessionPayment> calculatePayments(Map<UserId, Integer> netScores) {
        List<UserId> creditors = netScores.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(Map.Entry::getKey)
                .toList();
        List<UserId> debtors = netScores.entrySet().stream()
                .filter(entry -> entry.getValue() < 0)
                .map(Map.Entry::getKey)
                .toList();
        Map<UserId, Integer> remainingBalances = new HashMap<>(netScores);
        List<SessionPayment> payments = new ArrayList<>();

        // Match equal and opposite balances before splitting the remaining payments.
        for (UserId creditor : creditors) {
            for (UserId debtor : debtors) {
                if (remainingBalances.get(creditor) > 0
                        && remainingBalances.get(creditor).equals(-remainingBalances.get(debtor))) {
                    int amount = remainingBalances.get(creditor);
                    payments.add(new SessionPayment(debtor, creditor, amount));
                    remainingBalances.put(creditor, 0);
                    remainingBalances.put(debtor, 0);
                    break;
                }
            }
        }

        for (UserId creditor : creditors) {
            for (UserId debtor : debtors) {
                int amount = Math.min(remainingBalances.get(creditor), -remainingBalances.get(debtor));
                if (amount > 0) {
                    payments.add(new SessionPayment(debtor, creditor, amount));
                    remainingBalances.compute(creditor, (ignored, balance) -> balance - amount);
                    remainingBalances.compute(debtor, (ignored, balance) -> balance + amount);
                }
            }
        }

        return payments;
    }
}

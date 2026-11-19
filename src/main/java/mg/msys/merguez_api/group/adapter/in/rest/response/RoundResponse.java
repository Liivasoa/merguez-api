package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.util.List;
import java.util.UUID;

import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Round;

public record RoundResponse(int number, List<CombatResponse> combats) {

    public static RoundResponse from(Round round) {
        return new RoundResponse(
                round.number(),
                round.combats().stream().map(CombatResponse::from).toList());
    }

    public record CombatResponse(List<UUID> participantIds, UUID winnerId) {

        private static CombatResponse from(Combat combat) {
            return new CombatResponse(
                    combat.participants().stream().map(participant -> participant.value()).toList(),
                    combat.winner().map(winner -> winner.value()).orElse(null));
        }
    }
}

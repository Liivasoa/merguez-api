package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.Round;

public interface RoundRepository {

    /**
     * Persists the round for its occurrence and assigned number, including
     * every combat, its participants and optional winner.
     */
    void save(Round round);
}

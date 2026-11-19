package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.Round;

public interface RoundRepository {

    /**
     * Persists the round for its occurrence and assigned number, including
     * every combat, its participants and optional winner. If a round with the
     * same occurrence and number already exists, it is replaced.
     */
    void save(Round round);
}

package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.Round;

public interface RoundRepository {

    /**
     * Atomically creates a round only if its group has no round in progress.
     */
    void create(Round round);

    /**
     * Persists the round for its occurrence and assigned number, including
     * every combat, its participants and optional winner. If a round with the
     * same occurrence and number already exists, it is replaced.
     */
    void save(Round round);
}

package mg.msys.merguez_api.group.application.port.out;

import java.util.List;

import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public interface SaveClosedSessionOccurrencePort {
    /**
     * @throws mg.msys.merguez_api.group.application.exception.SessionClosureConflictException
     *         when the active occurrence or its loaded rounds have changed
     */
    void save(SessionOccurrence occurrence, List<Round> rounds);
}

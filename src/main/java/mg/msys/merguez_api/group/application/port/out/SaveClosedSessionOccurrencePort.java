package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public interface SaveClosedSessionOccurrencePort {
    void save(SessionOccurrence occurrence);
}

package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public interface SessionOccurrenceRepository {

    void save(SessionOccurrence occurrence);
}

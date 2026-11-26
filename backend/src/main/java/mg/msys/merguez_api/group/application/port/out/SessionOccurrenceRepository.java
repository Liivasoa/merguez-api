package mg.msys.merguez_api.group.application.port.out;

import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

public interface SessionOccurrenceRepository {

    boolean existsStartedByGroupId(GroupId groupId);

    /**
     * Implementations must atomically enforce that a group has at most one started occurrence.
     */
    void save(SessionOccurrence occurrence);
}

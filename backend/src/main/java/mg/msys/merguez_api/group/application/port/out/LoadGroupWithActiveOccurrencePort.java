package mg.msys.merguez_api.group.application.port.out;

import java.util.Optional;

import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;

public interface LoadGroupWithActiveOccurrencePort {

    /**
     * Loads the group with its persisted active occurrence and all its rounds,
     * in consecutive number order, including their combats and winners.
     * Returns empty if the group or its active occurrence does not exist.
     */
    Optional<Group> loadByGroupId(GroupId groupId);
}

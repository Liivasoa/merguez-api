package mg.msys.merguez_api.group.application.port.out;

import java.util.Optional;

import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;

public interface GroupRepository {

    void save(Group group);

    Optional<Group> findById(GroupId id);
}

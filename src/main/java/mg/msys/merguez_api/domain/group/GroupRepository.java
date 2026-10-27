package mg.msys.merguez_api.domain.group;

import java.util.Optional;

public interface GroupRepository {

    void save(Group group);

    Optional<Group> findById(GroupId id);
}

package mg.msys.merguez_api.infrastructure.group;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import mg.msys.merguez_api.domain.group.Group;
import mg.msys.merguez_api.domain.group.GroupId;
import mg.msys.merguez_api.domain.group.GroupRepository;

public class InMemoryGroupRepository implements GroupRepository {

    private final Map<GroupId, Group> groups = new ConcurrentHashMap<>();

    @Override
    public void save(Group group) {
        groups.put(group.id(), group);
    }

    @Override
    public Optional<Group> findById(GroupId id) {
        return Optional.ofNullable(groups.get(id));
    }
}

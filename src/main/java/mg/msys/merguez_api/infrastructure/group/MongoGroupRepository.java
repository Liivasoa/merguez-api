package mg.msys.merguez_api.infrastructure.group;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.domain.group.Group;
import mg.msys.merguez_api.domain.group.GroupId;
import mg.msys.merguez_api.domain.group.GroupRepository;

@Repository
class MongoGroupRepository implements GroupRepository {

    private final SpringDataGroupRepository repository;

    MongoGroupRepository(SpringDataGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Group group) {
        repository.save(GroupDocument.from(group));
    }

    @Override
    public Optional<Group> findById(GroupId id) {
        return repository.findById(id.value().toString()).map(GroupDocument::toGroup);
    }
}

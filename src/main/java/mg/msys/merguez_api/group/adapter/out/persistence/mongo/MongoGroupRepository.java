package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;

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

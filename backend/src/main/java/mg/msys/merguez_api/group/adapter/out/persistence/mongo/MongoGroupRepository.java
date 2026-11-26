package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupSummariesPort;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.GroupSummary;
import mg.msys.merguez_api.user.domain.model.UserId;

@Repository
class MongoGroupRepository implements GroupRepository, LoadGroupSummariesPort {

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

    @Override
    public List<GroupSummary> loadGroupSummaries(UserId userId) {
        return repository.findAllByMembersUserId(userId.value().toString()).stream()
                .map(GroupDocument::toGroup)
                .map(group -> group.summaryFor(userId))
                .flatMap(Optional::stream)
                .toList();
    }
}

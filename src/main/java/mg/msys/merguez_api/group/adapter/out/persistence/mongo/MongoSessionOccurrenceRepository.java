package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@Repository
class MongoSessionOccurrenceRepository implements SessionOccurrenceRepository {

    private final SpringDataSessionOccurrenceRepository repository;

    MongoSessionOccurrenceRepository(SpringDataSessionOccurrenceRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsStartedByGroupId(GroupId groupId) {
        return repository.existsByGroupIdAndStatus(groupId.value().toString(), "IN_PROGRESS");
    }

    @Override
    public void save(SessionOccurrence occurrence) {
        repository.save(SessionOccurrenceDocument.from(occurrence));
    }
}

package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import java.util.regex.Pattern;

import com.mongodb.MongoWriteException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@Repository
class MongoSessionOccurrenceRepository implements SessionOccurrenceRepository {

    private static final String ACTIVE_INDEX = "active_occurrence_per_group";
    private static final Pattern ACTIVE_INDEX_DUPLICATE = Pattern.compile(
            "^E11000 duplicate key error collection: \\S+\\.session-occurrences index: "
                    + ACTIVE_INDEX + " dup key: \\{");

    private final SpringDataSessionOccurrenceRepository repository;
    private final MongoTemplate mongoTemplate;
    private volatile boolean indexesInitialized;

    MongoSessionOccurrenceRepository(
            SpringDataSessionOccurrenceRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean existsStartedByGroupId(GroupId groupId) {
        ensureIndexes();
        return repository.existsByGroupIdAndStatus(
                groupId.value().toString(), SessionOccurrenceDocument.IN_PROGRESS_STATUS);
    }

    @Override
    public void save(SessionOccurrence occurrence) {
        ensureIndexes();
        try {
            repository.save(SessionOccurrenceDocument.from(occurrence));
        } catch (DuplicateKeyException exception) {
            if (isActiveIndexDuplicate(exception)) {
                throw new IllegalStateException("A session is already started in this group", exception);
            }
            throw exception;
        }
    }

    private static boolean isActiveIndexDuplicate(DuplicateKeyException exception) {
        return exception.getCause() instanceof MongoWriteException mongoFailure
                && mongoFailure.getError().getCode() == 11000
                && mongoFailure.getError().getMessage() != null
                && ACTIVE_INDEX_DUPLICATE.matcher(mongoFailure.getError().getMessage()).find();
    }

    private void ensureIndexes() {
        if (indexesInitialized) {
            return;
        }
        synchronized (this) {
            if (indexesInitialized) {
                return;
            }
            mongoTemplate.indexOps(SessionOccurrenceDocument.class)
                    .ensureIndex(new Index()
                            .on("groupId", ASC)
                            .unique()
                            .partial(PartialIndexFilter.of(Criteria.where("status")
                                    .is(SessionOccurrenceDocument.IN_PROGRESS_STATUS)))
                            .named(ACTIVE_INDEX));
            mongoTemplate.indexOps(SessionOccurrenceDocument.class)
                    .ensureIndex(new Index()
                            .on("groupId", ASC)
                            .on("startedAt", DESC)
                            .named("group_occurrence_history"));
            indexesInitialized = true;
        }
    }
}

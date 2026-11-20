package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@Repository
class MongoClosedSessionOccurrenceRepository implements SaveClosedSessionOccurrencePort {

    private final MongoTemplate mongoTemplate;

    MongoClosedSessionOccurrenceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(SessionOccurrence occurrence) {
        if (!occurrence.isClosed()) {
            throw new IllegalArgumentException("Session occurrence must be closed");
        }
        if (occurrence.persistenceToken().isEmpty() || !occurrence.closedWithRestoredRounds()) {
            throw new IllegalStateException("Session closure conflicts with the loaded rounds");
        }
        Document token = Document.parse(occurrence.persistenceToken().orElseThrow());
        long revision = token.get("roundsRevision", Number.class).longValue();
        Document filter = MongoSessionOccurrenceFilters.occurrenceFilter(occurrence)
                .append("_id", token.get("_id"));
        if (revision == 0) {
            filter.append("$and", List.of(new Document("$or", List.of(
                    new Document("roundsRevision", 0L),
                    new Document("roundsRevision", new Document("$exists", false))))));
        } else {
            filter.append("roundsRevision", revision);
        }
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter,
                new Document("$set", new Document("status", SessionOccurrenceDocument.CLOSED_STATUS)));
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Session closure conflicts with changed rounds or active occurrence");
        }
    }
}

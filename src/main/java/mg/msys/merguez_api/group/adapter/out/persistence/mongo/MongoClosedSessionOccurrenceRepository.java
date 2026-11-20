package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

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
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                MongoSessionOccurrenceFilters.occurrenceFilter(occurrence),
                new Document("$set", new Document("status", SessionOccurrenceDocument.CLOSED_STATUS)));
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Active session occurrence not found");
        }
    }
}

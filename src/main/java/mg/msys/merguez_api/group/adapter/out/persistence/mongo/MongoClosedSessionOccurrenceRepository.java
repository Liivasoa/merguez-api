package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@Repository
class MongoClosedSessionOccurrenceRepository implements SaveClosedSessionOccurrencePort {

    private final MongoTemplate mongoTemplate;

    MongoClosedSessionOccurrenceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(SessionOccurrence occurrence, List<Round> rounds) {
        if (!occurrence.isClosed()) {
            throw new IllegalArgumentException("Session occurrence must be closed");
        }
        if (rounds.isEmpty() || rounds.stream().anyMatch(round -> !round.occurrence().equals(occurrence))) {
            throw new IllegalStateException("Session closure conflicts with the loaded rounds");
        }
        Document filter = MongoSessionOccurrenceFilters.occurrenceFilter(occurrence);
        Document storedRounds = new Document("$objectToArray",
                new Document("$ifNull", List.of("$rounds", new Document())));
        filter.append("$expr", new Document("$eq", List.of(new Document("$size", storedRounds), rounds.size())));
        for (Round round : rounds) {
            String path = "rounds." + round.number();
            filter.append(path + ".number", round.number())
                    .append(path + ".status", round.status().name())
                    .append(path + ".combats", new Document("$size", round.combats().size()));
            for (int i = 0; i < round.combats().size(); i++) {
                Combat combat = round.combats().get(i);
                String combatPath = path + ".combats." + i;
                filter.append(combatPath + ".participants", combat.participants().stream()
                                .map(user -> user.value().toString()).toList())
                        .append(combatPath + ".winner",
                                combat.winner().map(user -> user.value().toString()).orElse(null));
            }
        }
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter,
                new Document("$set", new Document("status", SessionOccurrenceDocument.CLOSED_STATUS)));
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Session closure conflicts with changed rounds or active occurrence");
        }
    }
}

package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.exception.SessionClosureConflictException;
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
        List<Object> roundChecks = new ArrayList<>();
        roundChecks.add(equal(new Document("$size", new Document("$objectToArray", "$rounds")), rounds.size()));
        for (Round round : rounds) {
            List<Object> combatChecks = new ArrayList<>();
            combatChecks.add(equal(new Document("$size", "$$round.combats"), round.combats().size()));
            for (int i = 0; i < round.combats().size(); i++) {
                Combat combat = round.combats().get(i);
                Document combatMatches = new Document("$and", List.of(
                        equal("$$combat.participants", combat.participants().stream()
                                .map(user -> user.value().toString()).toList()),
                        equal(new Document("$ifNull", Arrays.asList("$$combat.winner", null)),
                                combat.winner().map(user -> user.value().toString()).orElse(null))));
                combatChecks.add(new Document("$let", new Document("vars", new Document("combat",
                        new Document("$arrayElemAt", List.of("$$round.combats", i))))
                        .append("in", ifType("$$combat", "object", combatMatches))));
            }
            Document roundMatches = new Document("$and", List.of(
                    ifType("$$round.number", "int", equal("$$round.number", round.number())),
                    equal("$$round.status", round.status().name()),
                    ifType("$$round.combats", "array", new Document("$and", combatChecks))));
            roundChecks.add(new Document("$let", new Document("vars", new Document("round",
                    "$rounds." + round.number()))
                    .append("in", ifType("$$round", "object", roundMatches))));
        }
        filter.append("$expr", ifType("$rounds", "object", new Document("$and", roundChecks)));
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter,
                new Document("$set", new Document("status", SessionOccurrenceDocument.CLOSED_STATUS)));
        if (result.getMatchedCount() == 0) {
            throw new SessionClosureConflictException("Session closure conflicts with changed rounds or active occurrence");
        }
    }

    private static Document equal(Object expression, Object expected) {
        return new Document("$eq", Arrays.asList(expression, new Document("$literal", expected)));
    }

    private static Document ifType(Object expression, String type, Object whenTrue) {
        return new Document("$cond", List.of(
                new Document("$eq", List.of(new Document("$type", expression), type)),
                whenTrue,
                false));
    }
}

package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@Repository
class MongoRoundRepository implements RoundRepository, LoadGroupWithActiveOccurrencePort {

    private final MongoTemplate mongoTemplate;
    private final SpringDataGroupRepository groups;

    MongoRoundRepository(MongoTemplate mongoTemplate, SpringDataGroupRepository groups) {
        this.mongoTemplate = mongoTemplate;
        this.groups = groups;
    }

    @Override
    public void save(Round round) {
        SessionOccurrence occurrence = round.occurrence();
        Document filter = activeOccurrenceFilter(occurrence.groupId())
                .append("scheduleId", occurrence.scheduleId().toString())
                .append("startedAt", Date.from(occurrence.startedAt()));
        if (occurrence.startedAt().equals(Date.from(occurrence.startedAt()).toInstant())) {
            filter.append("$or", List.of(
                    new Document("startedAtExact", occurrence.startedAt().toString()),
                    new Document("startedAtExact", new Document("$exists", false))));
        } else {
            filter.append("startedAtExact", occurrence.startedAt().toString());
        }
        Document storedRound = new Document("number", round.number())
                .append("combats", round.combats().stream()
                        .map(combat -> new Document("participants", combat.participants().stream()
                                        .map(user -> user.value().toString()).toList())
                                .append("winner", combat.winner().map(user -> user.value().toString()).orElse(null)))
                        .toList());
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter, new Document("$set", new Document("rounds." + round.number(), storedRound)));
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Active session occurrence not found");
        }
    }

    @Override
    public Optional<Group> loadByGroupId(GroupId groupId) {
        return groups.findById(groupId.value().toString()).flatMap(group -> {
            Document stored = mongoTemplate.getCollection("session-occurrences")
                    .find(activeOccurrenceFilter(groupId)).first();
            if (stored == null) {
                return Optional.empty();
            }
            String exactStart = stored.getString("startedAtExact");
            Instant startedAt = exactStart == null
                    ? stored.getDate("startedAt").toInstant() : Instant.parse(exactStart);
            SessionOccurrence occurrence = new SessionOccurrence(
                    groupId, UUID.fromString(stored.getString("scheduleId")), startedAt);
            Document storedRounds = stored.get("rounds", Document.class);
            List<Round> rounds = storedRounds == null ? List.of() : storedRounds.values().stream()
                    .map(value -> toRound((Document) value, occurrence))
                    .sorted(Comparator.comparingInt(Round::number))
                    .toList();
            return Optional.of(group.toGroup(occurrence, rounds));
        });
    }

    private static Document activeOccurrenceFilter(GroupId groupId) {
        return new Document("groupId", groupId.value().toString())
                .append("status", SessionOccurrenceDocument.IN_PROGRESS_STATUS);
    }

    private static Round toRound(Document stored, SessionOccurrence occurrence) {
        List<Combat> combats = stored.getList("combats", Document.class).stream()
                .map(combat -> new Combat(combat.getList("participants", String.class).stream()
                                .map(MongoRoundRepository::userId).toList(),
                        Optional.ofNullable(combat.getString("winner")).map(MongoRoundRepository::userId)))
                .toList();
        return new Round(occurrence, stored.getInteger("number"), combats);
    }

    private static UserId userId(String value) {
        return new UserId(UUID.fromString(value));
    }
}

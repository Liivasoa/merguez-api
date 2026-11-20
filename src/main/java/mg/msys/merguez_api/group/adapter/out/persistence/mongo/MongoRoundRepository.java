package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
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
        Document filter = occurrenceFilter(round.occurrence());
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter, new Document("$set", new Document("rounds." + round.number(), toStoredRound(round))));
        if (result.getMatchedCount() == 0) {
            throw new IllegalStateException("Active session occurrence not found");
        }
    }

    @Override
    public void replace(Round expected, Round updated) {
        if (!expected.occurrence().equals(updated.occurrence()) || expected.number() != updated.number()) {
            throw new IllegalArgumentException("Replacement must identify the same round");
        }
        String path = "rounds." + expected.number();
        Document filter = occurrenceFilter(expected.occurrence()).append(path, toStoredRound(expected));
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter, new Document("$set", new Document(path, toStoredRound(updated))));
        if (result.getMatchedCount() == 0) {
            throw new RoundOperationRefusedException(
                    new IllegalStateException("Round was changed or the active session is no longer available"));
        }
    }

    @Override
    public void create(Round round) {
        Document rounds = new Document("$objectToArray",
                new Document("$ifNull", List.of("$rounds", new Document())));
        Document activeRounds = new Document("$filter", new Document("input", rounds)
                .append("as", "round")
                .append("cond", new Document("$eq", List.of("$$round.v.status", RoundStatus.IN_PROGRESS.name()))));
        Document filter = occurrenceFilter(round.occurrence())
                .append("rounds." + round.number(), new Document("$exists", false))
                .append("$expr", new Document("$eq", List.of(new Document("$size", activeRounds), 0)));
        var result = mongoTemplate.getCollection("session-occurrences").updateOne(
                filter, new Document("$set", new Document("rounds." + round.number(), toStoredRound(round))));
        if (result.getMatchedCount() == 0) {
            throw new RoundOperationRefusedException(
                    new IllegalStateException("Round creation conflicts with the active session state"));
        }
    }

    private static Document occurrenceFilter(SessionOccurrence occurrence) {
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
        return filter;
    }

    private static Document toStoredRound(Round round) {
        return new Document("number", round.number())
                .append("status", round.status().name())
                .append("combats", round.combats().stream().map(MongoRoundRepository::toStoredCombat).toList());
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
        List<Combat> combats =
                stored.getList("combats", Document.class).stream().map(MongoRoundRepository::toCombat).toList();
        String status = Objects.requireNonNull(stored.getString("status"), "Persisted round status is required");
        return new Round(occurrence, stored.getInteger("number"), RoundStatus.valueOf(status), combats);
    }

    private static Document toStoredCombat(Combat combat) {
        List<String> participants = combat.participants().stream()
                .map(user -> user.value().toString())
                .toList();
        String winner = combat.winner().map(user -> user.value().toString()).orElse(null);
        return new Document("participants", participants).append("winner", winner);
    }

    private static Combat toCombat(Document stored) {
        List<UserId> participants = stored.getList("participants", String.class).stream()
                .map(MongoRoundRepository::userId)
                .toList();
        Optional<UserId> winner = Optional.ofNullable(stored.getString("winner"))
                .map(MongoRoundRepository::userId);
        return new Combat(participants, winner);
    }

    private static UserId userId(String value) {
        return new UserId(UUID.fromString(value));
    }
}

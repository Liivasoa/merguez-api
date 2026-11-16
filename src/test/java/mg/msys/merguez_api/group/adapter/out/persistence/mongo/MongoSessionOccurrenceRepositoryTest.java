package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Testcontainers
@Import({MongoGroupRepository.class, MongoSessionOccurrenceRepository.class})
class MongoSessionOccurrenceRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    SessionOccurrenceRepository occurrenceRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    void clearOccurrences() {
        if (!mongoTemplate.collectionExists("session-occurrences")) {
            mongoTemplate.createCollection("session-occurrences");
        }
        mongoTemplate.getCollection("session-occurrences").deleteMany(new Document());
    }

    @Test
    @DisplayName("A started occurrence is stored separately without changing its scheduled group")
    void savedOccurrenceIsStoredSeparatelyWithUtcStartInstant() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(member));
        Session scheduledSession =
                group.createSession(admin, "Evening practice", DayOfWeek.TUESDAY, LocalTime.of(18, 30));
        groupRepository.save(group);
        Document plannedGroupBefore = mongoTemplate.getCollection("groups")
                .find(new Document("_id", group.id().value().toString()))
                .first();
        Instant startedAt = Instant.parse("2026-03-04T05:06:07.123Z");
        SessionOccurrence occurrence = new SessionOccurrence(group.id(), scheduledSession.id(), startedAt);

        occurrenceRepository.save(occurrence);

        Document occurrenceDocument =
                mongoTemplate.getCollection("session-occurrences").find().first();
        Document plannedGroupAfter = mongoTemplate.getCollection("groups")
                .find(new Document("_id", group.id().value().toString()))
                .first();
        assertNotNull(occurrenceDocument);
        assertAll(
                () -> assertNotNull(occurrenceDocument.get("_id")),
                () -> assertNotEquals(scheduledSession.id().toString(), occurrenceDocument.get("_id").toString()),
                () -> assertEquals(
                        group.id().value().toString(), occurrenceDocument.get("groupId").toString()),
                () -> assertEquals(
                        scheduledSession.id().toString(), occurrenceDocument.get("scheduleId").toString()),
                () -> assertEquals("IN_PROGRESS", occurrenceDocument.getString("status")),
                () -> assertTrue(occurrenceDocument.get("startedAt") instanceof Date),
                () -> assertEquals(Date.from(startedAt), occurrenceDocument.get("startedAt")),
                () -> assertEquals(plannedGroupBefore, plannedGroupAfter));
    }

    @Test
    @DisplayName("Mongo indexes enforce one active occurrence per group, including concurrent saves")
    void activeOccurrenceIndexesEnforceUniquenessAndHistoryLookup() throws Exception {
        GroupId occupiedGroupId = GroupId.random();
        GroupId availableGroupId = GroupId.random();
        occurrenceRepository.save(new SessionOccurrence(
                occupiedGroupId, UUID.randomUUID(), Instant.parse("2026-03-04T05:06:07Z")));

        List<Boolean> saved = saveConcurrently(List.of(
                new SessionOccurrence(occupiedGroupId, UUID.randomUUID(), Instant.parse("2026-03-04T05:07:07Z")),
                new SessionOccurrence(availableGroupId, UUID.randomUUID(), Instant.parse("2026-03-04T05:08:07Z")),
                new SessionOccurrence(availableGroupId, UUID.randomUUID(), Instant.parse("2026-03-04T05:09:07Z"))));

        long occupiedGroupSaves = countActiveOccurrences(occupiedGroupId);
        long availableGroupSaves = countActiveOccurrences(availableGroupId);
        assertAll(
                () -> assertFalse(saved.get(0), "An already occupied group must reject another active occurrence"),
                () -> assertEquals(
                        1L, saved.subList(1, 3).stream().filter(Boolean::booleanValue).count()),
                () -> assertEquals(1L, occupiedGroupSaves),
                () -> assertEquals(1L, availableGroupSaves),
                () -> assertTrue(occurrenceRepository.existsStartedByGroupId(availableGroupId)));
    }

    @Test
    @DisplayName("Active occurrence and history indexes are initialized with the required options")
    void occurrenceIndexesAreInitializedWithRequiredOptions() {
        List<Document> indexes = mongoTemplate.getCollection("session-occurrences")
                .listIndexes()
                .into(new ArrayList<>());
        Document activeIndex = indexes.stream()
                .filter(index -> new Document("groupId", 1).equals(index.get("key"))
                        && new Document("status", "IN_PROGRESS").equals(index.get("partialFilterExpression")))
                .findFirst()
                .orElse(null);
        Document historyIndex = indexes.stream()
                .filter(index -> new Document("groupId", 1).append("startedAt", -1)
                        .equals(index.get("key")))
                .findFirst()
                .orElse(null);

        assertNotNull(activeIndex, "Expected a partial index on groupId for IN_PROGRESS occurrences");
        assertTrue(Boolean.TRUE.equals(activeIndex.get("unique")), "Expected the active index to be unique");
        assertNotNull(historyIndex, "Expected a groupId/startedAt history index");
        assertNull(historyIndex.get("unique"), "Expected the history index to be non-unique");
        assertNull(historyIndex.get("partialFilterExpression"), "Expected the history index to be unfiltered");
    }

    private List<Boolean> saveConcurrently(List<SessionOccurrence> occurrences) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(occurrences.size());
        CountDownLatch ready = new CountDownLatch(occurrences.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> saves = occurrences.stream()
                    .map(occurrence -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(30, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Timed out waiting to start concurrent saves");
                        }
                        try {
                            occurrenceRepository.save(occurrence);
                            return true;
                        } catch (DuplicateKeyException exception) {
                            return false;
                        }
                    }))
                    .toList();
            assertTrue(ready.await(30, TimeUnit.SECONDS), "Concurrent saves did not become ready");
            start.countDown();
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> save : saves) {
                results.add(save.get(30, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Concurrent saves did not terminate");
        }
    }

    private long countActiveOccurrences(GroupId groupId) {
        return mongoTemplate.getCollection("session-occurrences")
                .countDocuments(new Document("groupId", groupId.value().toString())
                        .append("status", "IN_PROGRESS"));
    }
}

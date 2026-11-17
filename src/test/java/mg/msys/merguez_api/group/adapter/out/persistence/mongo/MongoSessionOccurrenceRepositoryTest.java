package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import mg.msys.merguez_api.group.adapter.in.rest.GroupController;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LoadGroupWithActiveOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.usecase.StartSessionUseCase;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Testcontainers
@Import({MongoGroupRepository.class, MongoSessionOccurrenceRepository.class})
@ComponentScan(
        basePackageClasses = MongoGroupRepository.class,
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {RoundRepository.class, LoadGroupWithActiveOccurrencePort.class}))
class MongoSessionOccurrenceRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    GroupRepository groupRepository;

    @Autowired
    SessionOccurrenceRepository occurrenceRepository;

    @Autowired(required = false)
    RoundRepository roundRepository;

    @Autowired(required = false)
    LoadGroupWithActiveOccurrencePort activeOccurrenceLoader;

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
    @DisplayName("A saved round and its replacement retain occurrence, participants and optional winners")
    void savedRoundAndReplacementRestoreCombatsAndWinners() {
        assertAll(
                () -> assertNotNull(roundRepository, "A Mongo round repository must be available"),
                () -> assertNotNull(activeOccurrenceLoader, "A Mongo active occurrence loader must be available"));

        UserId admin = new UserId(UUID.randomUUID());
        UserId first = new UserId(UUID.randomUUID());
        UserId second = new UserId(UUID.randomUUID());
        UserId third = new UserId(UUID.randomUUID());
        UserId fourth = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(first, second, third, fourth));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.of(18, 30));
        SessionOccurrence occurrence =
                new SessionOccurrence(group.id(), session.id(), Instant.parse("2026-03-04T05:06:07.123Z"));
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);

        Round initial = new Round(occurrence, 1, List.of(
                new Combat(List.of(first, second), Optional.of(first)),
                new Combat(List.of(third, fourth), Optional.empty())));
        roundRepository.save(initial);

        Group restored = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
        assertEquals(List.of(occurrence), restored.activeOccurrences());
        assertEquals(List.of(initial), restored.rounds(occurrence));

        Round replacement = new Round(occurrence, 1, List.of(
                new Combat(List.of(first, third), Optional.empty()),
                new Combat(List.of(second, fourth), Optional.of(fourth))));
        roundRepository.save(replacement);

        Group restoredAfterReplacement = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
        assertEquals(List.of(occurrence), restoredAfterReplacement.activeOccurrences());
        assertEquals(List.of(replacement), restoredAfterReplacement.rounds(occurrence));
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

    @Test
    void activeIndexConflictIsTranslatedAndRetainsMongoFailure() {
        GroupId groupId = GroupId.random();
        occurrenceRepository.save(new SessionOccurrence(groupId, UUID.randomUUID(), Instant.now()));

        IllegalStateException conflict = assertThrows(IllegalStateException.class, () ->
                occurrenceRepository.save(new SessionOccurrence(groupId, UUID.randomUUID(), Instant.now())));

        assertTrue(hasCause(conflict, DuplicateKeyException.class), "The Mongo duplicate key must remain available");
        assertTrue(hasCause(conflict, com.mongodb.MongoWriteException.class), "The Mongo write failure must remain available");
        assertEquals(1L, countActiveOccurrences(groupId));
    }

    @Test
    void duplicateKeyFromAnotherMongoIndexIsNotTranslatedToActiveConflict() {
        UUID scheduleId = UUID.randomUUID();
        occurrenceRepository.save(new SessionOccurrence(GroupId.random(), scheduleId, Instant.now()));
        var collection = mongoTemplate.getCollection("session-occurrences");
        String index = collection.createIndex(new Document("scheduleId", 1),
                new com.mongodb.client.model.IndexOptions().unique(true).name("test_unique_schedule"));
        try {
            DuplicateKeyException failure = assertThrows(DuplicateKeyException.class, () ->
                    occurrenceRepository.save(new SessionOccurrence(GroupId.random(), scheduleId, Instant.now())));
            assertTrue(failure.getMessage().contains("test_unique_schedule"));
        } finally {
            collection.dropIndex(index);
        }
    }

    @Test
    void unrelatedPersistenceFailureIsNotTranslatedToActiveConflict() {
        SpringDataSessionOccurrenceRepository delegate = mock(SpringDataSessionOccurrenceRepository.class);
        var failure = new DataAccessResourceFailureException("database unavailable");
        when(delegate.save(any(SessionOccurrenceDocument.class))).thenThrow(failure);
        var repository = new MongoSessionOccurrenceRepository(delegate, mongoTemplate);

        var thrown = assertThrows(DataAccessResourceFailureException.class, () ->
                repository.save(new SessionOccurrence(GroupId.random(), UUID.randomUUID(), Instant.now())));

        assertSame(failure, thrown);
    }

    @Test
    void startRaceReturnsSameProblemAsPrecheckConflict() throws Exception {
        UserId admin = new UserId(UUID.randomUUID());
        Group group = Group.create("Training group", admin, List.of(new UserId(UUID.randomUUID())));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.of(18, 30));
        groupRepository.save(group);
        SessionOccurrence winningOccurrence = new SessionOccurrence(group.id(), session.id(), Instant.now());
        occurrenceRepository.save(winningOccurrence);
        MockMvc baseline = startMvc(occurrenceRepository);
        String expectedProblem = baseline.perform(post("/api/v1/groups/{groupId}/sessions/{sessionId}/start",
                        group.id().value(), session.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requesterId\":\"%s\"}".formatted(admin.value())))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("A session is already started in this group."))
                .andReturn().getResponse().getContentAsString();
        mongoTemplate.getCollection("session-occurrences").deleteMany(new Document());

        int[] calls = new int[2];
        SessionOccurrenceRepository racingRepository = new SessionOccurrenceRepository() {
            @Override
            public boolean existsStartedByGroupId(GroupId id) {
                calls[0]++;
                boolean exists = occurrenceRepository.existsStartedByGroupId(id);
                assertFalse(exists, "The losing request must pass the real precheck");
                occurrenceRepository.save(winningOccurrence);
                return exists;
            }

            @Override
            public void save(SessionOccurrence occurrence) {
                calls[1]++;
                occurrenceRepository.save(occurrence);
            }
        };
        MockMvc racing = startMvc(racingRepository);

        assertAll(
                () -> racing.perform(post("/api/v1/groups/{groupId}/sessions/{sessionId}/start",
                                group.id().value(), session.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterId\":\"%s\"}".formatted(admin.value())))
                        .andExpect(status().isConflict())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(content().json(expectedProblem, JsonCompareMode.STRICT)),
                () -> assertEquals(1, calls[0]),
                () -> assertEquals(1, calls[1]),
                () -> assertEquals(1L, countActiveOccurrences(group.id())));
    }

    private MockMvc startMvc(SessionOccurrenceRepository occurrences) {
        return MockMvcBuilders.standaloneSetup(new GroupController(
                mock(CreateGroupUseCase.class),
                mock(CreateSessionUseCase.class),
                mock(GetGroupSessionsUseCase.class),
                new StartSessionUseCase(groupRepository, occurrences))).build();
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable cause = failure.getCause(); cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
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
                        } catch (IllegalStateException exception) {
                            assertTrue(hasCause(exception, DuplicateKeyException.class),
                                    "Only Mongo active-index conflicts may be rejected");
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

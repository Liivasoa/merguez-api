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
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.application.port.out.SaveClosedSessionOccurrencePort;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.application.command.AddRoundCommand;
import mg.msys.merguez_api.group.application.command.CloseSessionCommand;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.usecase.AddRoundUseCase;
import mg.msys.merguez_api.group.application.usecase.CloseRoundUseCase;
import mg.msys.merguez_api.group.application.usecase.CloseSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.usecase.StartSessionUseCase;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
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
                classes = {RoundRepository.class, LoadGroupWithActiveOccurrencePort.class,
                        SaveClosedSessionOccurrencePort.class}))
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

    @Autowired(required = false)
    SaveClosedSessionOccurrencePort closedOccurrenceSaver;

    @Autowired
    SpringDataSessionOccurrenceRepository storedOccurrences;

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
        Document storedOccurrence = mongoTemplate.getCollection("session-occurrences")
                .find(new Document("groupId", group.id().value().toString())).first();
        assertEquals("IN_PROGRESS", storedOccurrence.get("rounds", Document.class)
                .get("1", Document.class).getString("status"));
        assertEquals(List.of(occurrence), restored.activeOccurrences());
        assertEquals(List.of(initial), restored.rounds(occurrence));
        assertThrows(IllegalStateException.class, () -> restored.addRound(admin, occurrence, List.of(
                new Combat(List.of(first, second), Optional.empty()))));

        Round replacement = new Round(occurrence, 1, List.of(
                new Combat(List.of(first, third), Optional.empty()),
                new Combat(List.of(second, fourth), Optional.of(fourth))));
        roundRepository.save(replacement);

        Group restoredAfterReplacement = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
        assertEquals(List.of(occurrence), restoredAfterReplacement.activeOccurrences());
        assertEquals(List.of(replacement), restoredAfterReplacement.rounds(occurrence));
    }

    @ParameterizedTest(name = "{0}, historical revision: {1}")
    @CsvSource({"save, false", "save, true", "create, false", "create, true",
            "replace, false", "replace, true"})
    void roundWritesPreserveAbsentOrHistoricalRevisionAndOtherOccurrenceData(
            String operation, boolean historicalRevision) {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);

        Round previous = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        Round original = new Round(occurrence, 2, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        Round updated = new Round(occurrence, 2, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(member))));
        Document rounds = new Document("1", storedRound(previous));
        if (operation.equals("replace")) {
            rounds.put("2", storedRound(original));
        }
        Document fields = new Document("rounds", rounds)
                .append("legacyMetadata", new Document("keep", List.of("old", "data")));
        if (historicalRevision) {
            fields.put("roundsRevision", 37L);
        }
        var collection = mongoTemplate.getCollection("session-occurrences");
        Document filter = new Document("groupId", group.id().value().toString());
        assertEquals(1L, collection.updateOne(filter, new Document("$set", fields)).getModifiedCount());
        Document before = collection.find(filter).first();
        assertNotNull(before);
        assertEquals(historicalRevision, before.containsKey("roundsRevision"));

        switch (operation) {
            case "save" -> roundRepository.save(updated);
            case "create" -> roundRepository.create(updated);
            case "replace" -> roundRepository.replace(original, updated);
            default -> throw new IllegalArgumentException("Unknown operation: " + operation);
        }

        Document after = collection.find(filter).first();
        Document expected = new Document(before);
        expected.put("rounds", new Document(before.get("rounds", Document.class))
                .append("2", storedRound(updated)));
        assertNotNull(after);
        assertAll(
                () -> assertEquals(historicalRevision, after.containsKey("roundsRevision"),
                        "A missing revision must remain absent"),
                () -> assertEquals(before.get("roundsRevision"), after.get("roundsRevision"),
                        "A historical revision must remain unchanged"),
                () -> assertEquals(expected, after, "All other occurrence data must be preserved"),
                () -> assertEquals(List.of(previous, updated),
                        activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence)));
    }

    private static Document storedRound(Round round) {
        return new Document("number", round.number())
                .append("status", round.status().name())
                .append("combats", round.combats().stream()
                        .map(combat -> new Document("participants", combat.participants().stream()
                                .map(user -> user.value().toString()).toList())
                                .append("winner", combat.winner().map(user -> user.value().toString()).orElse(null)))
                        .toList());
    }

    @Test
    void savesAndReloadsClosedOccurrenceWithoutLosingItsRoundsOrActiveSlot() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round firstRound = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        Round secondRound = new Round(occurrence, 2, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.empty())));
        roundRepository.save(firstRound);
        roundRepository.save(secondRound);

        var collection = mongoTemplate.getCollection("session-occurrences");
        Document before = collection.find(new Document("groupId", group.id().value().toString())).first();
        assertNotNull(before);
        String id = before.getString("_id");
        assertNotNull(id);
        assertTrue(occurrenceRepository.existsStartedByGroupId(group.id()));
        Group loaded = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
        SessionOccurrence loadedOccurrence = loaded.activeOccurrences().getFirst();
        assertEquals(List.of(firstRound, secondRound), loaded.rounds(loadedOccurrence));
        List<Round> snapshot = loaded.rounds(loadedOccurrence);
        var closure = loaded.closeSession(admin, loadedOccurrence);
        assertTrue(closure.occurrence().isClosed());
        assertEquals(Map.of(admin, 1, member, -1), closure.netScores());

        assertNotNull(closedOccurrenceSaver, "A Mongo closed occurrence saver must be available");
        closedOccurrenceSaver.save(closure.occurrence(), snapshot);

        SessionOccurrenceDocument restored = storedOccurrences.findById(id).orElseThrow();
        Document after = collection.find(new Document("_id", id)).first();
        assertNotNull(after);
        assertAll(
                () -> assertEquals("CLOSED", restored.status()),
                () -> assertEquals(id, restored.id()),
                () -> assertEquals(group.id().value().toString(), restored.groupId()),
                () -> assertEquals(session.id().toString(), restored.scheduleId()),
                () -> assertEquals(occurrence.startedAt().toString(), restored.startedAtExact()),
                () -> assertEquals(before.get("rounds"), after.get("rounds")),
                () -> assertEquals(1L, collection.countDocuments(new Document("groupId", group.id().value().toString()))),
                () -> assertFalse(occurrenceRepository.existsStartedByGroupId(group.id())),
                () -> assertTrue(activeOccurrenceLoader.loadByGroupId(group.id()).isEmpty()));

        SessionOccurrence next = new SessionOccurrence(
                group.id(), session.id(), occurrence.startedAt().plusSeconds(1));
        occurrenceRepository.save(next);
        assertAll(
                () -> assertTrue(occurrenceRepository.existsStartedByGroupId(group.id())),
                () -> assertEquals(2L, collection.countDocuments(new Document("groupId", group.id().value().toString()))),
                () -> assertEquals(after, collection.find(new Document("_id", id)).first()),
                () -> assertEquals(List.of(next),
                        activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().activeOccurrences()));
    }

    @Test
    void refusesSessionClosureWhenAnOpenRoundIsAddedAfterLoading() {
        assertClosureRefusesConcurrentRoundChange(true);
    }

    @Test
    void refusesSessionClosureWhenAClosedRoundIsModifiedAfterLoading() {
        assertClosureRefusesConcurrentRoundChange(false);
    }

    @Test
    void refusesSessionClosureWhenDirectWriteAddsRoundWithoutChangingRevision() {
        assertClosureRefusesDirectRoundChange(true, false);
    }

    @Test
    void refusesSessionClosureWhenDirectWriteModifiesRoundWithoutChangingRevision() {
        assertClosureRefusesDirectRoundChange(false, false);
    }

    @Test
    void refusesSessionClosureWhenDirectWriteAddsRoundToLegacyOccurrence() {
        assertClosureRefusesDirectRoundChange(true, true);
    }

    @Test
    void refusesSessionClosureWhenDirectWriteModifiesRoundInLegacyOccurrence() {
        assertClosureRefusesDirectRoundChange(false, true);
    }

    @Test
    void refusesSessionClosureWhenDirectWriteReplacesRoundStatusWithArray() {
        assertClosureRefusesScalarReplacedWithArray(false);
    }

    @Test
    void refusesSessionClosureWhenDirectWriteReplacesCombatWinnerWithArray() {
        assertClosureRefusesScalarReplacedWithArray(true);
    }

    private void assertClosureRefusesScalarReplacedWithArray(boolean winner) {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round initial = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        roundRepository.save(initial);

        var collection = mongoTemplate.getCollection("session-occurrences");
        Document filter = new Document("groupId", group.id().value().toString());
        collection.updateOne(filter, new Document("$set", new Document("roundsRevision", 37L)));
        Document before = collection.find(filter).first();
        assertNotNull(before);
        assertNotNull(before.get("roundsRevision"));
        AtomicReference<Document> afterConcurrentWrite = new AtomicReference<>();
        LoadGroupWithActiveOccurrencePort racingLoader = id -> {
            Group snapshot = activeOccurrenceLoader.loadByGroupId(id).orElseThrow();
            assertEquals(List.of(initial), snapshot.rounds(snapshot.activeOccurrences().getFirst()));
            String path = winner ? "rounds.1.combats.0.winner" : "rounds.1.status";
            List<String> replacement = winner
                    ? List.of(admin.value().toString(), member.value().toString())
                    : List.of("CLOSED", "IN_PROGRESS");
            assertEquals(1L, collection.updateOne(filter,
                    new Document("$set", new Document(path, replacement))).getModifiedCount());
            Document changed = collection.find(filter).first();
            assertNotNull(changed);
            assertEquals(before.get("roundsRevision"), changed.get("roundsRevision"));
            Document changedRound = changed.get("rounds", Document.class).get("1", Document.class);
            Object changedValue = winner
                    ? changedRound.getList("combats", Document.class).getFirst().get("winner")
                    : changedRound.get("status");
            assertEquals(replacement, changedValue);
            afterConcurrentWrite.set(changed);
            return Optional.of(snapshot);
        };
        LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        CloseSessionUseCase useCase =
                new CloseSessionUseCase(racingLoader, groupRepository, closedOccurrenceSaver, logger);

        assertAll(
                () -> {
                    RuntimeException conflict = assertThrows(RuntimeException.class, () -> useCase.execute(
                            new CloseSessionCommand(group.id(), admin, session.id())));
                    String description = (conflict.getClass().getSimpleName() + " " + conflict.getMessage())
                            .toLowerCase(java.util.Locale.ROOT);
                    assertTrue(description.contains("conflict") || description.contains("changed")
                                    || description.contains("modified") || description.contains("concurrent")
                                    || description.contains("stale"),
                            "The refusal must explicitly identify the concurrent change");
                },
                () -> assertNotNull(afterConcurrentWrite.get(), "The real concurrent write must have completed"),
                () -> assertEquals(afterConcurrentWrite.get(), collection.find(filter).first(),
                        "A failed closure must preserve the directly updated occurrence"),
                () -> assertEquals("IN_PROGRESS", collection.find(filter).first().getString("status")),
                () -> assertEquals(1L, collection.countDocuments(filter)),
                () -> verifyNoInteractions(logger));
    }

    private void assertClosureRefusesDirectRoundChange(boolean addRound, boolean legacy) {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round initial = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        var collection = mongoTemplate.getCollection("session-occurrences");
        Document filter = new Document("groupId", group.id().value().toString());
        if (legacy) {
            collection.updateOne(filter, new Document("$set", new Document("rounds.1", storedRound(initial))));
        } else {
            roundRepository.save(initial);
            collection.updateOne(filter, new Document("$set", new Document("roundsRevision", 37L)));
        }
        Document before = collection.find(filter).first();
        assertNotNull(before);
        assertEquals(legacy, !before.containsKey("roundsRevision"));
        AtomicReference<Document> afterConcurrentWrite = new AtomicReference<>();
        LoadGroupWithActiveOccurrencePort racingLoader = id -> {
            Group snapshot = activeOccurrenceLoader.loadByGroupId(id).orElseThrow();
            assertEquals(List.of(initial), snapshot.rounds(snapshot.activeOccurrences().getFirst()));
            Document storedRound = new Document("number", addRound ? 2 : 1)
                    .append("status", "CLOSED")
                    .append("combats", List.of(new Document("participants",
                            List.of(admin.value().toString(), member.value().toString()))
                            .append("winner", member.value().toString())));
            assertEquals(1L, collection.updateOne(filter, new Document("$set",
                    new Document("rounds." + (addRound ? 2 : 1), storedRound))).getModifiedCount());
            Document changed = collection.find(filter).first();
            assertNotNull(changed);
            assertEquals(before.get("roundsRevision"), changed.get("roundsRevision"));
            assertNotEquals(before.get("rounds"), changed.get("rounds"));
            afterConcurrentWrite.set(changed);
            return Optional.of(snapshot);
        };
        LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        CloseSessionUseCase useCase =
                new CloseSessionUseCase(racingLoader, groupRepository, closedOccurrenceSaver, logger);

        assertAll(
                () -> {
                    RuntimeException conflict = assertThrows(RuntimeException.class, () -> useCase.execute(
                            new CloseSessionCommand(group.id(), admin, session.id())));
                    String description = (conflict.getClass().getSimpleName() + " " + conflict.getMessage())
                            .toLowerCase(java.util.Locale.ROOT);
                    assertTrue(description.contains("conflict") || description.contains("changed")
                                    || description.contains("modified") || description.contains("concurrent")
                                    || description.contains("stale"),
                            "The refusal must explicitly identify the concurrent change");
                },
                () -> assertNotNull(afterConcurrentWrite.get()),
                () -> assertEquals(afterConcurrentWrite.get(), collection.find(filter).first(),
                        "A failed closure must not modify the directly updated occurrence"),
                () -> verifyNoInteractions(logger));
    }

    @Test
    void closesUnchangedLegacyOccurrenceWithoutRevisionAndPreservesAdditionalFields() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round initial = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        var collection = mongoTemplate.getCollection("session-occurrences");
        Document filter = new Document("groupId", group.id().value().toString());
        collection.updateOne(filter, new Document("$set", new Document("rounds.1", storedRound(initial))
                .append("legacyMetadata", new Document("keep", "value"))));
        Document before = collection.find(filter).first();
        assertNotNull(before);
        assertFalse(before.containsKey("roundsRevision"));
        LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        CloseSessionUseCase useCase =
                new CloseSessionUseCase(activeOccurrenceLoader, groupRepository, closedOccurrenceSaver, logger);

        var result = useCase.execute(new CloseSessionCommand(group.id(), admin, session.id()));

        assertTrue(result.occurrence().isClosed());
        Document expected = new Document(before);
        expected.put("status", "CLOSED");
        assertEquals(expected, collection.find(filter).first());
        org.mockito.Mockito.verify(logger, org.mockito.Mockito.times(2)).log(any(), org.mockito.ArgumentMatchers.same(result));
    }

    private void assertClosureRefusesConcurrentRoundChange(boolean addRound) {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = group.startSession(admin, session);
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round initial = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, member), Optional.of(admin))));
        roundRepository.save(initial);
        Round changed = addRound
                ? new Round(occurrence, 2, List.of(new Combat(List.of(admin, member), Optional.empty())))
                : new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                        new Combat(List.of(admin, member), Optional.of(member))));
        List<Round> expectedRounds = addRound ? List.of(initial, changed) : List.of(changed);
        var collection = mongoTemplate.getCollection("session-occurrences");
        Document filter = new Document("groupId", group.id().value().toString());
        Document before = collection.find(filter).first();
        assertNotNull(before);
        AtomicReference<Document> afterConcurrentWrite = new AtomicReference<>();
        LoadGroupWithActiveOccurrencePort racingLoader = id -> {
            Group snapshot = activeOccurrenceLoader.loadByGroupId(id).orElseThrow();
            assertEquals(List.of(initial), snapshot.rounds(snapshot.activeOccurrences().getFirst()));
            if (addRound) {
                roundRepository.create(changed);
            } else {
                roundRepository.replace(initial, changed);
            }
            assertEquals(expectedRounds, activeOccurrenceLoader.loadByGroupId(id).orElseThrow().rounds(occurrence));
            Document updated = collection.find(filter).first();
            assertNotNull(updated);
            assertEquals("IN_PROGRESS", updated.getString("status"));
            assertNotEquals(before.get("rounds"), updated.get("rounds"));
            afterConcurrentWrite.set(updated);
            return Optional.of(snapshot);
        };
        LogSessionClosurePort logger = mock(LogSessionClosurePort.class);
        CloseSessionUseCase useCase =
                new CloseSessionUseCase(racingLoader, groupRepository, closedOccurrenceSaver, logger);

        assertAll(
                () -> {
                    RuntimeException conflict = assertThrows(RuntimeException.class, () -> useCase.execute(
                            new CloseSessionCommand(group.id(), admin, session.id())));
                    String description = (conflict.getClass().getSimpleName() + " " + conflict.getMessage())
                            .toLowerCase(java.util.Locale.ROOT);
                    assertTrue(description.contains("conflict") || description.contains("changed")
                                    || description.contains("modified") || description.contains("concurrent")
                                    || description.contains("stale"),
                            "The refusal must explicitly identify the concurrent change");
                },
                () -> assertNotNull(afterConcurrentWrite.get(), "The real concurrent write must have completed"),
                () -> assertEquals(afterConcurrentWrite.get(), collection.find(filter).first(),
                        "The closure must preserve the occurrence and the concurrently written rounds"),
                () -> assertEquals("IN_PROGRESS", collection.find(filter).first().getString("status")),
                () -> assertEquals(afterConcurrentWrite.get().get("rounds"), collection.find(filter).first().get("rounds")),
                () -> assertEquals(1L, collection.countDocuments(filter)),
                () -> assertEquals(expectedRounds,
                        activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence)),
                () -> verifyNoInteractions(logger));
    }

    @Test
    void closesRoundAtomicallyAndRejectsStaleUpdates() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId first = new UserId(UUID.randomUUID());
        UserId second = new UserId(UUID.randomUUID());
        UserId third = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(first, second, third));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = new SessionOccurrence(group.id(), session.id(), Instant.now());
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round initial = new Round(occurrence, 1, List.of(
                new Combat(List.of(admin, first), Optional.empty()),
                new Combat(List.of(second, third), Optional.of(third))));
        roundRepository.create(initial);
        Round closed = new Round(occurrence, 1, RoundStatus.CLOSED, List.of(
                new Combat(List.of(admin, first), Optional.of(first)),
                new Combat(List.of(second, third), Optional.empty())));

        roundRepository.replace(initial, closed);
        assertEquals(List.of(closed),
                activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence));
        assertThrows(RoundOperationRefusedException.class, () -> roundRepository.replace(initial, initial));
        assertEquals(List.of(closed),
                activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence));
    }

    @Test
    void closeEndpointPersistsResultsAndAllowsNextRound() throws Exception {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = new SessionOccurrence(group.id(), session.id(), Instant.now());
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        Round initial = new Round(occurrence, 1, List.of(
                new Combat(List.of(admin, member), Optional.empty())));
        roundRepository.create(initial);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GroupController(
                mock(CreateGroupUseCase.class),
                mock(CreateSessionUseCase.class),
                mock(GetGroupSessionsUseCase.class),
                mock(StartSessionUseCase.class),
                new AddRoundUseCase(activeOccurrenceLoader, roundRepository),
                null,
                new CloseRoundUseCase(activeOccurrenceLoader, roundRepository))).build();

        mvc.perform(post("/api/v1/groups/{groupId}/rounds/1/close", group.id().value())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"requesterId":"%s","combats":[{"participantIds":["%s","%s"],"winnerId":"%s"}]}
                        """.formatted(admin.value(), admin.value(), member.value(), member.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.combats[0].winnerId").value(member.value().toString()));

        Round stored = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence).getFirst();
        assertEquals(RoundStatus.CLOSED, stored.status());
        assertEquals(Optional.of(member), stored.combats().getFirst().winner());
        Round next = new AddRoundUseCase(activeOccurrenceLoader, roundRepository).execute(
                new AddRoundCommand(group.id(), admin, List.of(initial.combats().getFirst())));
        assertEquals(2, next.number());
    }

    @Test
    @DisplayName("Persisted rounds and assignments support continued session operations")
    void restoredRoundsDetermineNextNumberAndAvailableParticipants() {
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

        Round firstRound = new Round(occurrence, 1, RoundStatus.COMPLETED, List.of(
                new Combat(List.of(third, fourth), Optional.of(fourth))));
        Round secondRound = new Round(occurrence, 2, RoundStatus.COMPLETED, List.of(
                new Combat(List.of(first, second), Optional.empty())));
        roundRepository.save(secondRound);
        roundRepository.save(firstRound);

        Group restored = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
        assertEquals(List.of(occurrence), restored.activeOccurrences());
        assertEquals(List.of(firstRound, secondRound), restored.rounds(occurrence));

        Combat conflictingCombat = new Combat(List.of(first, third), Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> restored.addCombat(admin, secondRound, conflictingCombat));
        assertEquals(List.of(firstRound, secondRound), restored.rounds(occurrence));

        Combat availableCombat = new Combat(List.of(third, fourth), Optional.empty());
        Round updatedSecondRound = restored.addCombat(admin, secondRound, availableCombat);
        assertEquals(List.of(secondRound.combats().get(0), availableCombat), updatedSecondRound.combats());
        roundRepository.save(updatedSecondRound);

        Round nextRound = restored.addRound(admin, occurrence, List.of(
                new Combat(List.of(first, third), Optional.empty())));
        assertEquals(3, nextRound.number());
        assertEquals(List.of(firstRound, updatedSecondRound, nextRound), restored.rounds(occurrence));
        roundRepository.create(nextRound);
        assertEquals(List.of(firstRound, updatedSecondRound, nextRound),
                activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence));
    }

    @Test
    void simultaneousRoundCreationsAllowOnlyOneSuccess() throws Exception {
        UserId admin = new UserId(UUID.randomUUID());
        UserId first = new UserId(UUID.randomUUID());
        UserId second = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(first, second));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.of(18, 30));
        SessionOccurrence occurrence = new SessionOccurrence(group.id(), session.id(), Instant.now());
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        CountDownLatch loaded = new CountDownLatch(2);
        LoadGroupWithActiveOccurrencePort simultaneousLoader = id -> {
            Optional<Group> result = activeOccurrenceLoader.loadByGroupId(id);
            loaded.countDown();
            try {
                if (!loaded.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for concurrent round requests");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while loading rounds", exception);
            }
            return result;
        };
        AddRoundUseCase useCase = new AddRoundUseCase(simultaneousLoader, roundRepository);
        AddRoundCommand command = new AddRoundCommand(group.id(), admin,
                List.of(new Combat(List.of(first, second), Optional.empty())));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var requests = List.of(
                    executor.submit(() -> tryAddRound(useCase, command)),
                    executor.submit(() -> tryAddRound(useCase, command)));
            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> request : requests) {
                results.add(request.get(30, TimeUnit.SECONDS));
            }
            assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
            Group restored = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
            assertEquals(1, restored.rounds(restored.activeOccurrences().getFirst()).size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    private static boolean tryAddRound(AddRoundUseCase useCase, AddRoundCommand command) {
        try {
            useCase.execute(command);
            return true;
        } catch (RoundOperationRefusedException exception) {
            return false;
        }
    }

    @Test
    @DisplayName("A legacy active occurrence without rounds can be continued")
    void legacyActiveOccurrenceWithoutRoundsCanBeContinued() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.of(18, 30));
        SessionOccurrence occurrence =
                new SessionOccurrence(group.id(), session.id(), Instant.parse("2026-03-04T05:06:07.123Z"));
        groupRepository.save(group);
        occurrenceRepository.save(occurrence);
        mongoTemplate.getCollection("session-occurrences").updateOne(
                new Document("groupId", group.id().value().toString()),
                new Document("$unset", new Document("rounds", "").append("startedAtExact", "")));

        Group restored = activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow();
        assertEquals(List.of(occurrence), restored.activeOccurrences());
        assertTrue(restored.rounds(occurrence).isEmpty());
        Round firstRound = restored.addRound(admin, occurrence, List.of(
                new Combat(List.of(admin, member), Optional.empty())));
        assertEquals(1, firstRound.number());
        roundRepository.save(firstRound);
        assertEquals(List.of(firstRound),
                activeOccurrenceLoader.loadByGroupId(group.id()).orElseThrow().rounds(occurrence));
    }

    @Test
    @DisplayName("Loading an active occurrence requires both its group and occurrence")
    void activeOccurrenceLoaderReturnsEmptyWhenGroupOrOccurrenceIsMissing() {
        UserId admin = new UserId(UUID.randomUUID());
        Group group = Group.create("Tuesday dojo", admin, List.of(new UserId(UUID.randomUUID())));
        groupRepository.save(group);
        assertTrue(activeOccurrenceLoader.loadByGroupId(group.id()).isEmpty());

        GroupId missingGroup = GroupId.random();
        occurrenceRepository.save(new SessionOccurrence(
                missingGroup, UUID.randomUUID(), Instant.parse("2026-03-04T05:06:07.123Z")));
        assertTrue(activeOccurrenceLoader.loadByGroupId(missingGroup).isEmpty());
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

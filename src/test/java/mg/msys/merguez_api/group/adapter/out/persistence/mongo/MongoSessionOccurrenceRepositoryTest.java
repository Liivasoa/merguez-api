package mg.msys.merguez_api.group.adapter.out.persistence.mongo;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@DataMongoTest
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
}

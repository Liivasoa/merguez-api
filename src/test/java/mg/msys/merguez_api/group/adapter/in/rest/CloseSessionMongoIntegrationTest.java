package mg.msys.merguez_api.group.adapter.in.rest;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.port.out.LogSessionClosurePort;
import mg.msys.merguez_api.group.application.port.out.RoundRepository;
import mg.msys.merguez_api.group.application.port.out.SessionOccurrenceRepository;
import mg.msys.merguez_api.group.domain.model.Combat;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.Round;
import mg.msys.merguez_api.group.domain.model.RoundStatus;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Testcontainers
class CloseSessionMongoIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private GroupRepository groups;
    @Autowired
    private SessionOccurrenceRepository occurrences;
    @Autowired
    private RoundRepository rounds;
    @Autowired
    private MongoTemplate mongoTemplate;
    @MockitoSpyBean
    private LogSessionClosurePort closureLogger;

    @Test
    void closesSessionThroughHttpAndPersistsClosedOccurrence() throws Exception {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        Instant startedAt = Instant.parse("2026-11-16T21:13:53Z");
        SessionOccurrence occurrence = new SessionOccurrence(group.id(), session.id(), startedAt);
        groups.save(group);
        occurrences.save(occurrence);
        Round initial = new Round(occurrence, 1, List.of(new Combat(List.of(admin, member), Optional.empty())));
        rounds.create(initial);
        rounds.replace(initial, new Round(occurrence, 1, RoundStatus.CLOSED,
                List.of(new Combat(List.of(admin, member), Optional.of(admin)))));

        MockMvcBuilders.webAppContextSetup(context).build()
                .perform(post("/api/v1/groups/{groupId}/sessions/{sessionId}/close",
                                group.id().value(), session.id())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterId\":\"%s\"}".formatted(member.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrence.groupId").value(group.id().value().toString()))
                .andExpect(jsonPath("$.occurrence.scheduleId").value(session.id().toString()))
                .andExpect(jsonPath("$.occurrence.startedAt").value(startedAt.toString()))
                .andExpect(jsonPath("$.netScores.length()").value(2))
                .andExpect(jsonPath("$.netScores['" + admin.value() + "']").value(1))
                .andExpect(jsonPath("$.netScores['" + member.value() + "']").value(-1))
                .andExpect(jsonPath("$.payments.length()").value(1))
                .andExpect(jsonPath("$.payments[0].debtorId").value(member.value().toString()))
                .andExpect(jsonPath("$.payments[0].creditorId").value(admin.value().toString()))
                .andExpect(jsonPath("$.payments[0].amount").value(1));

        Document stored = mongoTemplate.getCollection("session-occurrences")
                .find(new Document("groupId", group.id().value().toString())).first();
        assertNotNull(stored);
        assertEquals("CLOSED", stored.getString("status"));
    }

    @Test
    void refusesToCloseSessionWithOpenRoundWithoutChangingMongoDocumentsOrLogging() {
        UserId admin = new UserId(UUID.randomUUID());
        UserId member = new UserId(UUID.randomUUID());
        Group group = Group.create("Training", admin, List.of(member));
        Session session = group.createSession(admin, "Practice", DayOfWeek.TUESDAY, LocalTime.NOON);
        SessionOccurrence occurrence = new SessionOccurrence(
                group.id(), session.id(), Instant.parse("2026-11-16T21:13:53Z"));
        groups.save(group);
        occurrences.save(occurrence);
        rounds.create(new Round(occurrence, 1, List.of(new Combat(List.of(admin, member), Optional.empty()))));

        var groupCollection = mongoTemplate.getCollection("groups");
        var occurrenceCollection = mongoTemplate.getCollection("session-occurrences");
        Document groupFilter = new Document("_id", group.id().value().toString());
        Document occurrenceFilter = new Document("groupId", group.id().value().toString());
        Document groupBefore = groupCollection.find(groupFilter).first();
        Document occurrenceBefore = occurrenceCollection.find(occurrenceFilter).first();
        assertNotNull(groupBefore);
        assertNotNull(occurrenceBefore);
        assertEquals("IN_PROGRESS", occurrenceBefore.getString("status"));
        assertEquals("IN_PROGRESS", occurrenceBefore.get("rounds", Document.class)
                .get("1", Document.class).getString("status"));

        assertAll(
                () -> MockMvcBuilders.webAppContextSetup(context).build()
                        .perform(post("/api/v1/groups/{groupId}/sessions/{sessionId}/close",
                                        group.id().value(), session.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"requesterId\":\"%s\"}".formatted(member.value())))
                        .andExpect(status().isConflict())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.status").value(409))
                        .andExpect(jsonPath("$.detail").value("The session closure request was refused."))
                        .andExpect(content().string(not(containsString(
                                "All rounds must be closed before closing the session")))),
                () -> assertEquals(groupBefore, groupCollection.find(groupFilter).first()),
                () -> assertEquals(occurrenceBefore, occurrenceCollection.find(occurrenceFilter).first()),
                () -> verifyNoInteractions(closureLogger));
    }
}

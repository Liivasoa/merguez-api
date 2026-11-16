package mg.msys.merguez_api.group.adapter.in.rest;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import mg.msys.merguez_api.group.application.command.CreateGroupCommand;
import mg.msys.merguez_api.group.application.command.CreateSessionCommand;
import mg.msys.merguez_api.group.application.command.StartSessionCommand;
import mg.msys.merguez_api.group.application.port.out.GroupRepository;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.usecase.StartSessionUseCase;
import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.exception.InvalidGroupException;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.Session;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;
import mg.msys.merguez_api.user.domain.model.UserId;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGroupUseCase createGroupUseCase;

    @MockitoBean
    private CreateSessionUseCase createSessionUseCase;

    @MockitoBean
    private GetGroupSessionsUseCase getGroupSessionsUseCase;

    @MockitoBean
    private StartSessionUseCase startSessionUseCase;

    @Test
    @DisplayName("POST a scheduled session start forwards its identifiers and returns the recorded occurrence")
    void startScheduledSessionForwardsRequestAndReturnsRecordedOccurrence() throws Exception {
        GroupId groupId = GroupId.random();
        UUID scheduledSessionId = UUID.randomUUID();
        UserId requester = new UserId(UUID.randomUUID());
        Instant startedAt = Instant.parse("2026-11-16T21:13:53Z");
        when(startSessionUseCase.execute(any(StartSessionCommand.class)))
                .thenReturn(new SessionOccurrence(groupId, scheduledSessionId, startedAt));
        String requestBody = """
                {
                  "requesterId": "%s"
                }
                """.formatted(requester.value());

        mockMvc.perform(post(
                        "/api/v1/groups/{groupId}/sessions/{sessionId}/start",
                        groupId.value(),
                        scheduledSessionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groupId").value(groupId.value().toString()))
                .andExpect(jsonPath("$.scheduleId").value(scheduledSessionId.toString()))
                .andExpect(jsonPath("$.startedAt").value(startedAt.toString()));

        verify(startSessionUseCase).execute(new StartSessionCommand(groupId, requester, scheduledSessionId));
    }

    @Test
    @DisplayName("POST a session start exposes an active-group refusal while another group can start")
    void startSessionRefusedForActiveGroupDoesNotPreventStartInAnotherGroup() {
        GroupId activeGroupId = GroupId.random();
        GroupId otherGroupId = GroupId.random();
        UUID refusedScheduleId = UUID.randomUUID();
        UUID allowedScheduleId = UUID.randomUUID();
        UserId activeGroupRequester = new UserId(UUID.randomUUID());
        UserId otherGroupRequester = new UserId(UUID.randomUUID());
        Instant startedAt = Instant.parse("2026-11-16T21:13:53Z");
        String sensitiveDetail = "private internal active session details";
        StartSessionCommand refusedCommand =
                new StartSessionCommand(activeGroupId, activeGroupRequester, refusedScheduleId);
        StartSessionCommand allowedCommand =
                new StartSessionCommand(otherGroupId, otherGroupRequester, allowedScheduleId);
        when(startSessionUseCase.execute(refusedCommand)).thenThrow(new IllegalStateException(sensitiveDetail));
        when(startSessionUseCase.execute(allowedCommand))
                .thenReturn(new SessionOccurrence(otherGroupId, allowedScheduleId, startedAt));

        assertAll(
                () -> mockMvc.perform(post(
                                "/api/v1/groups/{groupId}/sessions/{sessionId}/start",
                                activeGroupId.value(),
                                refusedScheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterId\":\"%s\"}".formatted(activeGroupRequester.value())))
                        .andExpect(status().isConflict())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.status").value(409))
                        .andExpect(jsonPath("$.detail").value("A session is already started in this group."))
                        .andExpect(content().string(not(containsString(sensitiveDetail)))),
                () -> mockMvc.perform(post(
                                "/api/v1/groups/{groupId}/sessions/{sessionId}/start",
                                otherGroupId.value(),
                                allowedScheduleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requesterId\":\"%s\"}".formatted(otherGroupRequester.value())))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.groupId").value(otherGroupId.value().toString()))
                        .andExpect(jsonPath("$.scheduleId").value(allowedScheduleId.toString()))
                        .andExpect(jsonPath("$.startedAt").value(startedAt.toString())),
                () -> verify(startSessionUseCase).execute(refusedCommand),
                () -> verify(startSessionUseCase).execute(allowedCommand));
    }

    @Test
    @DisplayName("POST and GET sessions for a missing group return 404 ProblemDetail without saving")
    void sessionOperationsForMissingGroupReturnNotFoundWithoutSaving() {
        GroupId groupId = GroupId.random();
        GroupRepository repository = mock(GroupRepository.class);
        when(repository.findById(groupId)).thenReturn(Optional.empty());
        MockMvc standaloneMvc = MockMvcBuilders.standaloneSetup(new GroupController(
                createGroupUseCase,
                new CreateSessionUseCase(repository),
                new GetGroupSessionsUseCase(repository)))
                .setControllerAdvice(new GroupExceptionHandler())
                .build();
        String requestBody = """
                {
                  "requesterId": "%s",
                  "name": "Evening training",
                  "day": "TUESDAY",
                  "startTime": "14:30"
                }
                """.formatted(UUID.randomUUID());

        assertAll(
                () -> standaloneMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.status").value(404)),
                () -> standaloneMvc.perform(get("/api/v1/groups/{groupId}/sessions", groupId.value()))
                        .andExpect(status().isNotFound())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("$.status").value(404)),
                () -> verify(repository, times(2)).findById(groupId),
                () -> verify(repository, never()).save(any()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sessionNames")
    @DisplayName("POST a group session forwards its group, requester and unchanged details")
    void createSessionForwardsRequestUnchanged(String caseName, String nameField, String expectedName)
            throws Exception {
        GroupId groupId = GroupId.random();
        UserId requester = new UserId(UUID.randomUUID());
        DayOfWeek day = DayOfWeek.TUESDAY;
        LocalTime startTime = LocalTime.of(14, 30);
        when(createSessionUseCase.execute(any(CreateSessionCommand.class)))
                .thenReturn(new Session(groupId, "Created session", day, startTime));
        String requestBody = """
                {
                  "requesterId": "%s",
                  "day": "TUESDAY",
                  "startTime": "14:30"%s
                }
                """.formatted(requester.value(), nameField);

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is2xxSuccessful());

        verify(createSessionUseCase).execute(
                new CreateSessionCommand(groupId, requester, expectedName, day, startTime));
    }

    private static Stream<Arguments> sessionNames() {
        return Stream.of(
                Arguments.of("explicit name", ", \"name\": \"Evening training\"", "Evening training"),
                Arguments.of("absent name", "", null),
                Arguments.of("empty name", ", \"name\": \"\"", ""));
    }

    @Test
    @DisplayName("POST a group session denied by the use case returns 403 ProblemDetail without sensitive details")
    void createSessionDeniedForNonMemberReturnsForbidden() throws Exception {
        GroupId groupId = GroupId.random();
        UserId requester = new UserId(UUID.randomUUID());
        String sensitiveDetail = "sensitive internal access details";
        when(createSessionUseCase.execute(any(CreateSessionCommand.class)))
                .thenThrow(new GroupAccessDeniedException(sensitiveDetail));
        String requestBody = """
                {
                  "requesterId": "%s",
                  "name": "Evening training",
                  "day": "TUESDAY",
                  "startTime": "14:30"
                }
                """.formatted(requester.value());

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(content().string(not(containsString(sensitiveDetail))));

        verify(createSessionUseCase).execute(new CreateSessionCommand(
                groupId, requester, "Evening training", DayOfWeek.TUESDAY, LocalTime.of(14, 30)));
    }

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("missingSessionScheduleFields")
    @DisplayName("POST a group session without a day or start time returns 400 and does not execute the use case")
    void createSessionWithoutRequiredScheduleFieldReturnsBadRequest(String missingField, String scheduleFields)
            throws Exception {
        GroupId groupId = GroupId.random();
        UserId requester = new UserId(UUID.randomUUID());
        String requestBody = """
                {
                  "requesterId": "%s",
                  %s
                }
                """.formatted(requester.value(), scheduleFields);

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createSessionUseCase);
    }

    private static Stream<Arguments> missingSessionScheduleFields() {
        return Stream.of(
                Arguments.of("day", "\"startTime\": \"14:30\""),
                Arguments.of("start time", "\"day\": \"TUESDAY\""));
    }

    @Test
    @DisplayName("GET group sessions includes the created session at the mocked application boundary")
    void getGroupSessionsIncludesCreatedSession() throws Exception {
        GroupId groupId = GroupId.random();
        UserId requester = new UserId(UUID.randomUUID());
        LocalTime startTime = LocalTime.of(14, 30);
        Session session = new Session(groupId, "Evening training", DayOfWeek.TUESDAY, startTime);
        when(createSessionUseCase.execute(any(CreateSessionCommand.class))).thenReturn(session);
        when(getGroupSessionsUseCase.execute(groupId)).thenReturn(List.of(session));
        String requestBody = """
                {
                  "requesterId": "%s",
                  "name": "Evening training",
                  "day": "TUESDAY",
                  "startTime": "14:30"
                }
                """.formatted(requester.value());

        mockMvc.perform(post("/api/v1/groups/{groupId}/sessions", groupId.value())
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is2xxSuccessful());
        verify(createSessionUseCase).execute(new CreateSessionCommand(
                groupId, requester, session.name(), session.day(), startTime));

        mockMvc.perform(get("/api/v1/groups/{groupId}/sessions", groupId.value()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$[0].groupId").value(groupId.value().toString()))
                .andExpect(jsonPath("$[0].id").value(session.id().toString()))
                .andExpect(jsonPath("$[0].name").value(session.name()))
                .andExpect(jsonPath("$[0].day").value("TUESDAY"))
                .andExpect(jsonPath("$[0].startTime").value(anyOf(is("14:30"), is("14:30:00"))));
        verify(getGroupSessionsUseCase).execute(groupId);
    }

    @Test
    @DisplayName("POST /api/v1/groups executes the use case and returns 201 Created with the mapped group")
    void createGroupReturnsCreatedWithMappedResponse() throws Exception {
        // Given a creator "Alice" and a participant "Bob"
        UserId creatorId = new UserId(UUID.randomUUID());
        UserId participantId = new UserId(UUID.randomUUID());
        // And the CreateGroup use case returns the group "Dojo du mardi"
        Group group = Group.create("Dojo du mardi", "Séances entre amis", creatorId, List.of(participantId));
        when(createGroupUseCase.execute(any(CreateGroupCommand.class))).thenReturn(group);
        // And a valid creation request with a description and Bob as a participant
        String requestBody = """
                {
                  "creatorId": "%s",
                  "name": "Dojo du mardi",
                  "description": "Séances entre amis",
                  "participantIds": ["%s"]
                }
                """.formatted(creatorId.value(), participantId.value());

        // When POST /api/v1/groups is called
        var result = mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Then the response is 201 Created
        result.andExpect(status().isCreated())
                // And the Location header points to the created group
                .andExpect(header().string("Location", "http://localhost/api/v1/groups/" + group.id().value()))
                // And the body contains the group id, name and description
                .andExpect(jsonPath("$.id").value(group.id().value().toString()))
                .andExpect(jsonPath("$.name").value("Dojo du mardi"))
                .andExpect(jsonPath("$.description").value("Séances entre amis"))
                // And the members list starts with Alice as ADMIN
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].userId").value(creatorId.value().toString()))
                .andExpect(jsonPath("$.members[0].role").value("ADMIN"))
                // And Bob follows as MEMBER
                .andExpect(jsonPath("$.members[1].userId").value(participantId.value().toString()))
                .andExpect(jsonPath("$.members[1].role").value("MEMBER"));
        // And the use case was executed with the command mapped from the request
        ArgumentCaptor<CreateGroupCommand> commandCaptor = ArgumentCaptor.forClass(CreateGroupCommand.class);
        verify(createGroupUseCase).execute(commandCaptor.capture());
        assertEquals(
                new CreateGroupCommand(creatorId, "Dojo du mardi", "Séances entre amis", List.of(participantId)),
                commandCaptor.getValue());
    }

    @ParameterizedTest(name = "name = [{0}]")
    @NullSource
    @ValueSource(strings = { "", "   " })
    @DisplayName("POST /api/v1/groups with an absent, empty or blank name returns 400 ProblemDetail and does not execute the use case")
    void createGroupWithMissingNameReturnsBadRequest(String name) throws Exception {
        // Given a creator "Alice" and a participant "Bob"
        UserId creatorId = new UserId(UUID.randomUUID());
        UserId participantId = new UserId(UUID.randomUUID());
        // And a creation request whose name is absent, empty or only whitespace
        String nameJson = name == null ? "null" : "\"" + name + "\"";
        String requestBody = """
                {
                  "creatorId": "%s",
                  "name": %s,
                  "participantIds": ["%s"]
                }
                """.formatted(creatorId.value(), nameJson, participantId.value());

        // When POST /api/v1/groups is called
        var result = mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Then the response is 400 Bad Request
        result.andExpect(status().isBadRequest())
                // And the body is an RFC 9457 ProblemDetail
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        // And the use case was never executed
        verifyNoInteractions(createGroupUseCase);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "absent", "empty", "creator only" })
    @DisplayName("POST /api/v1/groups without any participant other than the creator returns 400 ProblemDetail and does not execute the use case")
    void createGroupWithoutParticipantReturnsBadRequest(String case_) throws Exception {
        // Given a creator "Alice" with a valid group name
        UserId creatorId = new UserId(UUID.randomUUID());
        // And participantIds is absent, empty, or contains only Alice
        String participantIdsJson = switch (case_) {
            case "absent" -> "null";
            case "empty" -> "[]";
            default -> "[\"%s\"]".formatted(creatorId.value());
        };
        String requestBody = """
                {
                  "creatorId": "%s",
                  "name": "Dojo du mardi",
                  "participantIds": %s
                }
                """.formatted(creatorId.value(), participantIdsJson);

        // When POST /api/v1/groups is called
        var result = mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Then the response is 400 Bad Request
        result.andExpect(status().isBadRequest())
                // And the body is an RFC 9457 ProblemDetail
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        // And the use case was never executed
        verifyNoInteractions(createGroupUseCase);
    }

    @Test
    @DisplayName("POST /api/v1/groups without a creator returns 400 ProblemDetail and does not execute the use case")
    void createGroupWithoutCreatorReturnsBadRequest() throws Exception {
        // Given a participant "Bob" and no creator
        UserId participantId = new UserId(UUID.randomUUID());
        String requestBody = """
                {
                  "name": "Dojo du mardi",
                  "participantIds": ["%s"]
                }
                """.formatted(participantId.value());

        // When POST /api/v1/groups is called
        var result = mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Then the response is 400 Bad Request
        result.andExpect(status().isBadRequest())
                // And the body is an RFC 9457 ProblemDetail
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        // And the use case was never executed
        verifyNoInteractions(createGroupUseCase);
    }

    @Test
    @DisplayName("POST /api/v1/groups refused by the domain returns 400 ProblemDetail carrying the domain message")
    void createGroupRefusedByDomainReturnsBadRequest() throws Exception {
        // Given a creator "Alice" and a participant "Bob"
        UserId creatorId = new UserId(UUID.randomUUID());
        UserId participantId = new UserId(UUID.randomUUID());
        // And the CreateGroup use case refuses the request with a domain exception
        when(createGroupUseCase.execute(any(CreateGroupCommand.class)))
                .thenThrow(new InvalidGroupException("Group name is required"));
        // And a syntactically valid creation request
        String requestBody = """
                {
                  "creatorId": "%s",
                  "name": "Dojo du mardi",
                  "participantIds": ["%s"]
                }
                """.formatted(creatorId.value(), participantId.value());

        // When POST /api/v1/groups is called
        var result = mockMvc.perform(post("/api/v1/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));

        // Then the response is 400 Bad Request
        result.andExpect(status().isBadRequest())
                // And the body is an RFC 9457 ProblemDetail
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                // And the detail carries the domain message
                .andExpect(jsonPath("$.detail").value("Group name is required"));
    }
}

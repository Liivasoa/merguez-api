package mg.msys.merguez_api.application.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import mg.msys.merguez_api.domain.group.CreateGroupCommand;
import mg.msys.merguez_api.domain.group.CreateGroupUseCase;
import mg.msys.merguez_api.domain.group.Group;
import mg.msys.merguez_api.domain.user.UserId;

@WebMvcTest(GroupController.class)
class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateGroupUseCase createGroupUseCase;

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
}

package mg.msys.merguez_api.group.adapter.in.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import mg.msys.merguez_api.group.adapter.in.rest.request.CreateGroupRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateSessionRequest;
import mg.msys.merguez_api.group.adapter.in.rest.response.GroupResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.SessionResponse;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final CreateGroupUseCase createGroupUseCase;
    private final CreateSessionUseCase createSessionUseCase;
    private final GetGroupSessionsUseCase getGroupSessionsUseCase;

    public GroupController(
            CreateGroupUseCase createGroupUseCase,
            CreateSessionUseCase createSessionUseCase,
            GetGroupSessionsUseCase getGroupSessionsUseCase) {
        this.createGroupUseCase = createGroupUseCase;
        this.createSessionUseCase = createSessionUseCase;
        this.getGroupSessionsUseCase = getGroupSessionsUseCase;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        Group group = createGroupUseCase.execute(request.toCommand());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(group.id().value())
                .toUri();
        return ResponseEntity.created(location).body(GroupResponse.from(group));
    }

    @PostMapping("/{groupId}/sessions")
    public ResponseEntity<Void> createSession(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSessionRequest request) {
        createSessionUseCase.execute(request.toCommand(new GroupId(groupId)));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/sessions")
    public List<SessionResponse> getGroupSessions(@PathVariable UUID groupId) {
        return getGroupSessionsUseCase.execute(new GroupId(groupId)).stream()
                .map(SessionResponse::from)
                .toList();
    }
}

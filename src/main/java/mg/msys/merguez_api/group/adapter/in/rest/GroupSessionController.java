package mg.msys.merguez_api.group.adapter.in.rest;

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

import mg.msys.merguez_api.group.adapter.in.rest.request.CloseSessionRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateSessionRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.StartSessionRequest;
import mg.msys.merguez_api.group.adapter.in.rest.response.SessionClosureResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.SessionOccurrenceResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.SessionResponse;
import mg.msys.merguez_api.group.application.port.in.CloseSessionUseCase;
import mg.msys.merguez_api.group.application.port.in.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.port.in.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.port.in.StartSessionUseCase;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.SessionClosureResult;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/sessions")
public class GroupSessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final GetGroupSessionsUseCase getGroupSessionsUseCase;
    private final StartSessionUseCase startSessionUseCase;
    private final CloseSessionUseCase closeSessionUseCase;

    public GroupSessionController(
            CreateSessionUseCase createSessionUseCase,
            GetGroupSessionsUseCase getGroupSessionsUseCase,
            StartSessionUseCase startSessionUseCase,
            CloseSessionUseCase closeSessionUseCase) {
        this.createSessionUseCase = createSessionUseCase;
        this.getGroupSessionsUseCase = getGroupSessionsUseCase;
        this.startSessionUseCase = startSessionUseCase;
        this.closeSessionUseCase = closeSessionUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> createSession(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSessionRequest request) {
        createSessionUseCase.execute(request.toCommand(new GroupId(groupId)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sessionId}/start")
    public ResponseEntity<?> startSession(
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody StartSessionRequest request) {
        var command = request.toCommand(new GroupId(groupId), sessionId);
        SessionOccurrence occurrence = startSessionUseCase.execute(command);
        return ResponseEntity.ok(SessionOccurrenceResponse.from(occurrence));
    }

    @PostMapping("/{sessionId}/close")
    public ResponseEntity<?> closeSession(
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CloseSessionRequest request) {
        var command = request.toCommand(new GroupId(groupId), sessionId);
        SessionClosureResult result = closeSessionUseCase.execute(command);
        return ResponseEntity.ok(SessionClosureResponse.from(result));
    }

    @GetMapping
    public List<SessionResponse> getGroupSessions(@PathVariable UUID groupId) {
        return getGroupSessionsUseCase.execute(new GroupId(groupId)).stream()
                .map(SessionResponse::from)
                .toList();
    }
}

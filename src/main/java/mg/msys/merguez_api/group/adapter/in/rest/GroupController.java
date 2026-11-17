package mg.msys.merguez_api.group.adapter.in.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import mg.msys.merguez_api.group.adapter.in.rest.request.AddCombatRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateGroupRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateRoundRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateSessionRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.StartSessionRequest;
import mg.msys.merguez_api.group.adapter.in.rest.response.GroupResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.RoundResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.SessionOccurrenceResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.SessionResponse;
import mg.msys.merguez_api.group.application.usecase.AddCombatUseCase;
import mg.msys.merguez_api.group.application.usecase.AddRoundUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.usecase.CreateSessionUseCase;
import mg.msys.merguez_api.group.application.usecase.GetGroupSessionsUseCase;
import mg.msys.merguez_api.group.application.usecase.StartSessionUseCase;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.group.domain.model.GroupId;
import mg.msys.merguez_api.group.domain.model.SessionOccurrence;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private static final Logger log = LoggerFactory.getLogger(GroupController.class);

    private final CreateGroupUseCase createGroupUseCase;
    private final CreateSessionUseCase createSessionUseCase;
    private final GetGroupSessionsUseCase getGroupSessionsUseCase;
    private final StartSessionUseCase startSessionUseCase;
    private final AddRoundUseCase addRoundUseCase;
    private final AddCombatUseCase addCombatUseCase;

    @Autowired
    public GroupController(
            CreateGroupUseCase createGroupUseCase,
            CreateSessionUseCase createSessionUseCase,
            GetGroupSessionsUseCase getGroupSessionsUseCase,
            StartSessionUseCase startSessionUseCase,
            AddRoundUseCase addRoundUseCase,
            AddCombatUseCase addCombatUseCase) {
        this.createGroupUseCase = createGroupUseCase;
        this.createSessionUseCase = createSessionUseCase;
        this.getGroupSessionsUseCase = getGroupSessionsUseCase;
        this.startSessionUseCase = startSessionUseCase;
        this.addRoundUseCase = addRoundUseCase;
        this.addCombatUseCase = addCombatUseCase;
    }

    public GroupController(
            CreateGroupUseCase createGroupUseCase,
            CreateSessionUseCase createSessionUseCase,
            GetGroupSessionsUseCase getGroupSessionsUseCase,
            StartSessionUseCase startSessionUseCase,
            AddRoundUseCase addRoundUseCase) {
        this(createGroupUseCase, createSessionUseCase, getGroupSessionsUseCase,
                startSessionUseCase, addRoundUseCase, null);
    }

    public GroupController(
            CreateGroupUseCase createGroupUseCase,
            CreateSessionUseCase createSessionUseCase,
            GetGroupSessionsUseCase getGroupSessionsUseCase,
            StartSessionUseCase startSessionUseCase) {
        this(createGroupUseCase, createSessionUseCase, getGroupSessionsUseCase, startSessionUseCase, null);
    }

    GroupController(
            CreateGroupUseCase createGroupUseCase,
            CreateSessionUseCase createSessionUseCase,
            GetGroupSessionsUseCase getGroupSessionsUseCase) {
        this(createGroupUseCase, createSessionUseCase, getGroupSessionsUseCase, null);
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

    @PostMapping("/{groupId}/rounds")
    public ResponseEntity<?> createRound(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateRoundRequest request) {
        try {
            var round = addRoundUseCase.execute(request.toCommand(new GroupId(groupId)));
            return ResponseEntity.status(HttpStatus.CREATED).body(RoundResponse.from(round));
        } catch (IllegalArgumentException ex) {
            log.debug("Round creation refused", ex);
            return refusedRoundRequest(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException ex) {
            log.debug("Round creation refused", ex);
            return refusedRoundRequest(HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/{groupId}/rounds/{roundNumber}/combats")
    public ResponseEntity<?> addCombat(
            @PathVariable UUID groupId,
            @PathVariable int roundNumber,
            @Valid @RequestBody AddCombatRequest request) {
        try {
            var round = addCombatUseCase.execute(request.toCommand(new GroupId(groupId), roundNumber));
            return ResponseEntity.ok(RoundResponse.from(round));
        } catch (IllegalArgumentException ex) {
            log.debug("Combat addition refused", ex);
            return refusedRoundRequest(HttpStatus.BAD_REQUEST);
        } catch (IllegalStateException ex) {
            log.debug("Combat addition refused", ex);
            return refusedRoundRequest(HttpStatus.CONFLICT);
        }
    }

    private ResponseEntity<ProblemDetail> refusedRoundRequest(HttpStatus status) {
        return ResponseEntity.status(status)
                .body(ProblemDetail.forStatusAndDetail(status, "The round request was refused."));
    }

    @PostMapping("/{groupId}/sessions/{sessionId}/start")
    public ResponseEntity<?> startSession(
            @PathVariable UUID groupId,
            @PathVariable UUID sessionId,
            @Valid @RequestBody StartSessionRequest request) {
        var command = request.toCommand(new GroupId(groupId), sessionId);
        SessionOccurrence occurrence;
        try {
            occurrence = startSessionUseCase.execute(command);
        } catch (IllegalStateException ex) {
            log.debug("Session start refused", ex);
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(
                    HttpStatus.CONFLICT, "A session is already started in this group.")).build();
        }
        return ResponseEntity.ok(SessionOccurrenceResponse.from(occurrence));
    }

    @GetMapping("/{groupId}/sessions")
    public List<SessionResponse> getGroupSessions(@PathVariable UUID groupId) {
        return getGroupSessionsUseCase.execute(new GroupId(groupId)).stream()
                .map(SessionResponse::from)
                .toList();
    }
}

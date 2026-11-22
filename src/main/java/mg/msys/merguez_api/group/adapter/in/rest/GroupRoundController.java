package mg.msys.merguez_api.group.adapter.in.rest;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import mg.msys.merguez_api.group.adapter.in.rest.request.AddCombatRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CloseRoundRequest;
import mg.msys.merguez_api.group.adapter.in.rest.request.CreateRoundRequest;
import mg.msys.merguez_api.group.adapter.in.rest.response.RoundResponse;
import mg.msys.merguez_api.group.application.port.in.AddCombatUseCase;
import mg.msys.merguez_api.group.application.port.in.AddRoundUseCase;
import mg.msys.merguez_api.group.application.port.in.CloseRoundUseCase;
import mg.msys.merguez_api.group.domain.model.GroupId;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/rounds")
public class GroupRoundController {

    private final AddRoundUseCase addRoundUseCase;
    private final AddCombatUseCase addCombatUseCase;
    private final CloseRoundUseCase closeRoundUseCase;

    public GroupRoundController(
            AddRoundUseCase addRoundUseCase,
            AddCombatUseCase addCombatUseCase,
            CloseRoundUseCase closeRoundUseCase) {
        this.addRoundUseCase = addRoundUseCase;
        this.addCombatUseCase = addCombatUseCase;
        this.closeRoundUseCase = closeRoundUseCase;
    }

    @PostMapping
    public ResponseEntity<RoundResponse> createRound(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateRoundRequest request) {
        var round = addRoundUseCase.execute(request.toCommand(new GroupId(groupId)));
        return ResponseEntity.status(HttpStatus.CREATED).body(RoundResponse.from(round));
    }

    @PostMapping("/{roundNumber}/combats")
    public ResponseEntity<RoundResponse> addCombat(
            @PathVariable UUID groupId,
            @PathVariable int roundNumber,
            @Valid @RequestBody AddCombatRequest request) {
        var round = addCombatUseCase.execute(request.toCommand(new GroupId(groupId), roundNumber));
        return ResponseEntity.ok(RoundResponse.from(round));
    }

    @PostMapping("/{roundNumber}/close")
    public ResponseEntity<RoundResponse> closeRound(
            @PathVariable UUID groupId,
            @PathVariable int roundNumber,
            @Valid @RequestBody CloseRoundRequest request) {
        var round = closeRoundUseCase.execute(request.toCommand(new GroupId(groupId), roundNumber));
        return ResponseEntity.ok(RoundResponse.from(round));
    }
}

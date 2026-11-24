package mg.msys.merguez_api.group.adapter.in.rest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import mg.msys.merguez_api.group.adapter.in.rest.request.CreateGroupRequest;
import mg.msys.merguez_api.group.adapter.in.rest.response.GroupResponse;
import mg.msys.merguez_api.group.adapter.in.rest.response.GroupSummaryResponse;
import mg.msys.merguez_api.group.application.port.in.CreateGroupUseCase;
import mg.msys.merguez_api.group.application.port.in.ListMyGroupsUseCase;
import mg.msys.merguez_api.group.domain.model.Group;
import mg.msys.merguez_api.user.domain.model.UserId;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final CreateGroupUseCase createGroupUseCase;
    private final ListMyGroupsUseCase listMyGroupsUseCase;

    public GroupController(CreateGroupUseCase createGroupUseCase, ListMyGroupsUseCase listMyGroupsUseCase) {
        this.createGroupUseCase = createGroupUseCase;
        this.listMyGroupsUseCase = listMyGroupsUseCase;
    }

    @GetMapping
    public List<GroupSummaryResponse> listMyGroups(@RequestParam UUID userId) {
        return listMyGroupsUseCase.execute(new UserId(userId)).stream()
                .map(GroupSummaryResponse::from)
                .toList();
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
}

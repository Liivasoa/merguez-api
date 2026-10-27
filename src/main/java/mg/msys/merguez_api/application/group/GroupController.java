package mg.msys.merguez_api.application.group;

import java.net.URI;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import mg.msys.merguez_api.domain.group.CreateGroupUseCase;
import mg.msys.merguez_api.domain.group.Group;

@RestController
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final CreateGroupUseCase createGroupUseCase;

    public GroupController(CreateGroupUseCase createGroupUseCase) {
        this.createGroupUseCase = createGroupUseCase;
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

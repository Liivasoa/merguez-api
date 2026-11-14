package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.util.List;
import java.util.UUID;

import mg.msys.merguez_api.group.domain.model.Group;

public record GroupResponse(UUID id, String name, String description, List<MemberResponse> members) {

    public static GroupResponse from(Group group) {
        return new GroupResponse(
                group.id().value(),
                group.name(),
                group.description().orElse(null),
                group.members().stream()
                        .map(MemberResponse::from)
                        .toList());
    }
}

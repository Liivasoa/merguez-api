package mg.msys.merguez_api.group.adapter.in.rest.response;

import java.util.UUID;

import mg.msys.merguez_api.group.domain.model.GroupMember;
import mg.msys.merguez_api.group.domain.model.Role;

public record MemberResponse(UUID userId, Role role) {

    public static MemberResponse from(GroupMember member) {
        return new MemberResponse(member.userId().value(), member.role());
    }
}
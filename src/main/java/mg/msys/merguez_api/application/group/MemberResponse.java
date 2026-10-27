package mg.msys.merguez_api.application.group;

import java.util.UUID;

import mg.msys.merguez_api.domain.group.GroupMember;
import mg.msys.merguez_api.domain.group.Role;

public record MemberResponse(UUID userId, Role role) {

    public static MemberResponse from(GroupMember member) {
        return new MemberResponse(member.userId().value(), member.role());
    }
}
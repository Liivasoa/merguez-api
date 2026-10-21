package mg.msys.merguez_api.domain.group;

import mg.msys.merguez_api.domain.user.UserId;

public record GroupMember(UserId userId, Role role) {
}

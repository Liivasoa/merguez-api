package mg.msys.merguez_api.group.application.exception;

import mg.msys.merguez_api.group.domain.model.GroupId;

public class GroupNotFoundException extends RuntimeException {

    public GroupNotFoundException(GroupId groupId) {
        super("Group not found: " + groupId);
    }
}

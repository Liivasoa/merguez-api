package mg.msys.merguez_api.group.application.exception;

import mg.msys.merguez_api.group.domain.model.GroupId;

public final class CurrentGroupSessionNotFoundException extends RuntimeException {

    public CurrentGroupSessionNotFoundException(GroupId groupId) {
        super("Current group session not found: " + groupId);
    }
}

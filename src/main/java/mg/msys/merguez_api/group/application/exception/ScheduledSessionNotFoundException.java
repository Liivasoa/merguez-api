package mg.msys.merguez_api.group.application.exception;

import java.util.UUID;

public final class ScheduledSessionNotFoundException extends RuntimeException {

    public ScheduledSessionNotFoundException(UUID sessionId) {
        super("Scheduled session not found: " + sessionId);
    }
}

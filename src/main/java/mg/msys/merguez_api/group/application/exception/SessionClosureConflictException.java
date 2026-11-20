package mg.msys.merguez_api.group.application.exception;

public final class SessionClosureConflictException extends RuntimeException {

    public SessionClosureConflictException(String message) {
        super(message);
    }
}

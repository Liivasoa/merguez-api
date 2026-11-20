package mg.msys.merguez_api.group.application.exception;

public final class SessionClosureRefusedException extends RuntimeException {

    public SessionClosureRefusedException(Throwable cause) {
        super("Session closure refused", cause);
    }
}

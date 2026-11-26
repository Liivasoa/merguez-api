package mg.msys.merguez_api.group.application.exception;

public final class SessionAlreadyStartedException extends RuntimeException {

    public SessionAlreadyStartedException() {
        super("A session is already started in this group");
    }

    public SessionAlreadyStartedException(Throwable cause) {
        super("A session is already started in this group", cause);
    }
}

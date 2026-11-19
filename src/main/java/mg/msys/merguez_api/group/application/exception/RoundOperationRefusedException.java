package mg.msys.merguez_api.group.application.exception;

public final class RoundOperationRefusedException extends RuntimeException {

    public RoundOperationRefusedException(Throwable cause) {
        super("Round operation refused", cause);
    }
}

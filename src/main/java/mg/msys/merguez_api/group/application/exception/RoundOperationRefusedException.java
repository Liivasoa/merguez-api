package mg.msys.merguez_api.group.application.exception;

public final class RoundOperationRefusedException extends RuntimeException {

    public enum Reason {
        INVALID_REQUEST,
        CONFLICT
    }

    private final Reason reason;

    public RoundOperationRefusedException(Reason reason, Throwable cause) {
        super("Round operation refused", cause);
        this.reason = reason;
    }

    /** @deprecated Specify the refusal reason at the application boundary. */
    @Deprecated
    public RoundOperationRefusedException(Throwable cause) {
        this(cause instanceof IllegalStateException ? Reason.CONFLICT : Reason.INVALID_REQUEST, cause);
    }

    public Reason reason() {
        return reason;
    }
}

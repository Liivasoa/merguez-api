package mg.msys.merguez_api.group.application.exception;

import java.util.Objects;

public final class RoundOperationRefusedException extends RuntimeException {

    public enum Reason {
        INVALID_REQUEST,
        CONFLICT
    }

    private final Reason reason;

    public RoundOperationRefusedException(Reason reason) {
        this(reason, null);
    }

    public RoundOperationRefusedException(Reason reason, Throwable cause) {
        super("Round operation refused", cause);
        this.reason = Objects.requireNonNull(reason);
    }

    public Reason reason() {
        return reason;
    }
}

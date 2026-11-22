package mg.msys.merguez_api.group.application.exception;

import java.util.Objects;

public final class RoundOperationRefusedException extends RuntimeException {

    public enum Reason {
        INVALID_REQUEST,
        CONFLICT
    }

    private final Reason reason;
    private final String detail;

    public RoundOperationRefusedException(Reason reason, String detail) {
        this(reason, detail, null);
    }

    public RoundOperationRefusedException(Reason reason, String detail, Throwable cause) {
        super("Round operation refused", cause);
        this.reason = Objects.requireNonNull(reason);
        this.detail = Objects.requireNonNull(detail);
    }

    public Reason reason() {
        return reason;
    }

    public String detail() {
        return detail;
    }
}

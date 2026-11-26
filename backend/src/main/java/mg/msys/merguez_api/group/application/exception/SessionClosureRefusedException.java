package mg.msys.merguez_api.group.application.exception;

import java.util.Objects;

public final class SessionClosureRefusedException extends RuntimeException {

    private final String detail;

    public SessionClosureRefusedException(String detail, Throwable cause) {
        super("Session closure refused", cause);
        this.detail = Objects.requireNonNull(detail);
    }

    public String detail() {
        return detail;
    }
}

package mg.msys.merguez_api.group.domain.exception;

public final class RoundNotInProgressException extends DomainConflictException {

    public RoundNotInProgressException() {
        super("Round must be in progress");
    }
}

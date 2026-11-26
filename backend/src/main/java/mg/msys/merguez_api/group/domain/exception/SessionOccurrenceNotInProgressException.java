package mg.msys.merguez_api.group.domain.exception;

public final class SessionOccurrenceNotInProgressException extends DomainConflictException {

    public SessionOccurrenceNotInProgressException() {
        super("Session occurrence must be in progress");
    }
}

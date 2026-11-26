package mg.msys.merguez_api.group.domain.exception;

public final class SessionClosureRejectedException extends DomainConflictException {

    public SessionClosureRejectedException(String message) {
        super(message);
    }
}

package mg.msys.merguez_api.group.domain.exception;

public final class SessionAlreadyStartedDomainException extends DomainConflictException {

    public SessionAlreadyStartedDomainException() {
        super("A session is already started in this group");
    }
}

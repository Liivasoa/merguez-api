package mg.msys.merguez_api.group.domain.exception;

public final class RoundAlreadyInProgressException extends DomainConflictException {

    public RoundAlreadyInProgressException() {
        super("A round is already in progress in this group");
    }
}

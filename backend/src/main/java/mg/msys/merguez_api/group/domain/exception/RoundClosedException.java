package mg.msys.merguez_api.group.domain.exception;

public final class RoundClosedException extends DomainConflictException {

    public RoundClosedException() {
        super("Round is closed");
    }
}

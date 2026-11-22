package mg.msys.merguez_api.group.domain.exception;

public abstract class DomainConflictException extends RuntimeException {

    protected DomainConflictException(String message) {
        super(message);
    }
}

package mg.msys.merguez_api.group.domain.exception;

public abstract class DomainValidationException extends RuntimeException {

    protected DomainValidationException(String message) {
        super(message);
    }
}

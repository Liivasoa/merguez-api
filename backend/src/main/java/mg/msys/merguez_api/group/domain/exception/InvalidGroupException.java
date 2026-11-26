package mg.msys.merguez_api.group.domain.exception;

public class InvalidGroupException extends DomainValidationException {

    public InvalidGroupException(String message) {
        super(message);
    }
}

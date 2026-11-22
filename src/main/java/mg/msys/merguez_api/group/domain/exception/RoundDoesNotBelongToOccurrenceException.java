package mg.msys.merguez_api.group.domain.exception;

public final class RoundDoesNotBelongToOccurrenceException extends DomainValidationException {

    public RoundDoesNotBelongToOccurrenceException() {
        super("Round must belong to this session occurrence");
    }
}

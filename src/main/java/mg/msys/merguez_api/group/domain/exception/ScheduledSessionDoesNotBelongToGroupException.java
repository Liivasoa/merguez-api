package mg.msys.merguez_api.group.domain.exception;

public final class ScheduledSessionDoesNotBelongToGroupException extends DomainValidationException {

    public ScheduledSessionDoesNotBelongToGroupException() {
        super("Scheduled session must belong to this group");
    }
}

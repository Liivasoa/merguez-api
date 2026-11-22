package mg.msys.merguez_api.group.domain.exception;

public final class DuplicateSessionScheduleException extends DomainConflictException {

    public DuplicateSessionScheduleException() {
        super("A session already exists at this day and start time");
    }
}

package mg.msys.merguez_api.group.domain.exception;

public class GroupAccessDeniedException extends RuntimeException {

    public GroupAccessDeniedException(String message) {
        super(message);
    }
}

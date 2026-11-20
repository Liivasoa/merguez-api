package mg.msys.merguez_api.group.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.exception.InvalidGroupException;

@RestControllerAdvice(assignableTypes = GroupController.class)
class GroupExceptionHandler {

    @ExceptionHandler(GroupNotFoundException.class)
    ProblemDetail handleGroupNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Group not found.");
    }

    @ExceptionHandler(GroupAccessDeniedException.class)
    ProblemDetail handleGroupAccessDenied() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(InvalidGroupException.class)
    ProblemDetail handleInvalidGroup(InvalidGroupException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(RoundOperationRefusedException.class)
    ProblemDetail handleRoundOperationRefused(RoundOperationRefusedException ex) {
        HttpStatus status = ex.getCause() instanceof IllegalStateException
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        String detail = ex.getCause() != null
                ? ex.getCause().getMessage()
                : "The round request was refused.";
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}

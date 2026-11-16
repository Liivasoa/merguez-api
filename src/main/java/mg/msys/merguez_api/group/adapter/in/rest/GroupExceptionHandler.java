package mg.msys.merguez_api.group.adapter.in.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.exception.InvalidGroupException;

@RestControllerAdvice(assignableTypes = GroupController.class)
class GroupExceptionHandler {

    @ExceptionHandler(GroupAccessDeniedException.class)
    ProblemDetail handleGroupAccessDenied() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(InvalidGroupException.class)
    ProblemDetail handleInvalidGroup(InvalidGroupException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}

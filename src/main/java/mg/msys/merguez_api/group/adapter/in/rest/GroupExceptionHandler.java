package mg.msys.merguez_api.group.adapter.in.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import mg.msys.merguez_api.group.application.exception.GroupNotFoundException;
import mg.msys.merguez_api.group.application.exception.RoundOperationRefusedException;
import mg.msys.merguez_api.group.application.exception.ScheduledSessionNotFoundException;
import mg.msys.merguez_api.group.application.exception.SessionAlreadyStartedException;
import mg.msys.merguez_api.group.application.exception.SessionClosureRefusedException;
import mg.msys.merguez_api.group.domain.exception.GroupAccessDeniedException;
import mg.msys.merguez_api.group.domain.exception.DuplicateSessionScheduleException;
import mg.msys.merguez_api.group.domain.exception.DomainValidationException;

@RestControllerAdvice(assignableTypes = {
        GroupController.class,
        GroupSessionController.class,
        GroupRoundController.class
})
public class GroupExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GroupExceptionHandler.class);

    @ExceptionHandler(GroupNotFoundException.class)
    ProblemDetail handleGroupNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Group not found.");
    }

    @ExceptionHandler(ScheduledSessionNotFoundException.class)
    ProblemDetail handleScheduledSessionNotFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Scheduled session not found in group.");
    }

    @ExceptionHandler(SessionAlreadyStartedException.class)
    ProblemDetail handleSessionAlreadyStarted() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "A session is already started in this group.");
    }

    @ExceptionHandler(GroupAccessDeniedException.class)
    ProblemDetail handleGroupAccessDenied() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
    }

    @ExceptionHandler(DomainValidationException.class)
    ProblemDetail handleDomainValidation(DomainValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateSessionScheduleException.class)
    ProblemDetail handleDuplicateSessionSchedule() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "A session already exists at this day and start time.");
    }

    @ExceptionHandler(RoundOperationRefusedException.class)
    ProblemDetail handleRoundOperationRefused(RoundOperationRefusedException ex) {
        HttpStatus status = ex.reason() == RoundOperationRefusedException.Reason.CONFLICT
                ? HttpStatus.CONFLICT
                : HttpStatus.BAD_REQUEST;
        log.debug("Round request refused", ex);
        return ProblemDetail.forStatusAndDetail(status, "The round request was refused.");
    }

    @ExceptionHandler(SessionClosureRefusedException.class)
    ProblemDetail handleSessionClosureRefused(SessionClosureRefusedException ex) {
        log.debug("Session closure refused", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The session closure request was refused.");
    }
}

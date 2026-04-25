package edu.tcu.cs.projectpulse.system;

import edu.tcu.cs.projectpulse.invitation.InvitationNotFoundException;
import edu.tcu.cs.projectpulse.rubric.RubricNameConflictException;
import edu.tcu.cs.projectpulse.rubric.RubricNotFoundException;
import edu.tcu.cs.projectpulse.section.SectionNameConflictException;
import edu.tcu.cs.projectpulse.section.SectionNotFoundException;
import edu.tcu.cs.projectpulse.team.TeamNameConflictException;
import edu.tcu.cs.projectpulse.team.TeamNotFoundException;
import edu.tcu.cs.projectpulse.user.UserNotFoundException;
import edu.tcu.cs.projectpulse.war.WARNotFoundException;
import edu.tcu.cs.projectpulse.peerevaluation.PeerEvaluationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RubricNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleRubricNotFound(RubricNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RubricNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result handleRubricNameConflict(RubricNameConflictException ex) {
        return new Result(false, StatusCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(TeamNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleTeamNotFound(TeamNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TeamNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result handleTeamNameConflict(TeamNameConflictException ex) {
        return new Result(false, StatusCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(SectionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleSectionNotFound(SectionNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SectionNameConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result handleSectionNameConflict(SectionNameConflictException ex) {
        return new Result(false, StatusCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvitationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleInvitationNotFound(InvitationNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleUserNotFound(UserNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(WARNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handleWARNotFound(WARNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PeerEvaluationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result handlePeerEvaluationNotFound(PeerEvaluationNotFoundException ex) {
        return new Result(false, StatusCode.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleIllegalArgument(IllegalArgumentException ex) {
        return new Result(false, StatusCode.INVALID_ARGUMENT, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result handleIllegalState(IllegalStateException ex) {
        return new Result(false, StatusCode.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return new Result(false, StatusCode.INVALID_ARGUMENT, "Malformed request body: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return new Result(false, StatusCode.INVALID_ARGUMENT,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return new Result(false, StatusCode.INVALID_ARGUMENT, "Validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleGeneric(Exception ex) {
        return new Result(false, StatusCode.INTERNAL_SERVER_ERROR, ex.getMessage());
    }
}

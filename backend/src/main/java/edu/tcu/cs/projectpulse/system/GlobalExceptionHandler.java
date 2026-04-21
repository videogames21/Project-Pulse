package edu.tcu.cs.projectpulse.system;

import edu.tcu.cs.projectpulse.rubric.RubricNameConflictException;
import edu.tcu.cs.projectpulse.rubric.RubricNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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

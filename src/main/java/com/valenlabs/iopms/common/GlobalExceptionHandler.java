package com.valenlabs.iopms.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;

/** Single place that turns exceptions into JSON error bodies. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<?> businessError(ApiException ex) {
        return error(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .orElse("Invalid input");
        return error(400, message);
    }

    // Bad JSON (invalid date/enum/number) must be a client error, not a 500.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable(HttpMessageNotReadableException ex) {
        return error(400, "Invalid request body — check field types and date format YYYY-MM-DD");
    }

    // Invalid path/query values: /interns/abc, ?date=nope, ?status=BOGUS, etc.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> typeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(400, "Invalid value for '" + ex.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<?> missingParam(MissingServletRequestParameterException ex) {
        return error(400, ex.getParameterName() + " is required");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<?> wrongMethod(HttpRequestMethodNotSupportedException ex) {
        return error(405, "Method not allowed");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied(AccessDeniedException ex) {
        return error(403, "You are not allowed to perform this action");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict(DataIntegrityViolationException ex) {
        return error(409, "Conflicting record, e.g. a duplicate entry already exists");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return error(500, "Something went wrong, please try again");
    }

    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of("status", status, "message", message, "timestamp", Instant.now().toString()));
    }
}

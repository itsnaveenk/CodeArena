package com.codearena.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.codearena.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> details = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        
        ErrorResponse response = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed",
            getPath(request),
            details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "VALIDATION_ERROR",
            ex.getMessage(),
            getPath(request),
            ex.getDetails()
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "AUTHENTICATION_ERROR",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailExists(
            EmailAlreadyExistsException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "CONFLICT",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "FORBIDDEN",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "NOT_FOUND",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(
            RateLimitExceededException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "RATE_LIMITED",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
            .body(response);
    }

    @ExceptionHandler(ContestRegistrationException.class)
    public ResponseEntity<ErrorResponse> handleContestRegistration(
            ContestRegistrationException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request)
        );
        HttpStatus status = "ALREADY_REGISTERED".equals(ex.getErrorCode()) 
            ? HttpStatus.CONFLICT 
            : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ContestSubmissionException.class)
    public ResponseEntity<ErrorResponse> handleContestSubmission(
            ContestSubmissionException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            ex.getErrorCode(),
            ex.getMessage(),
            getPath(request)
        );
        HttpStatus status = "RATE_LIMITED".equals(ex.getErrorCode()) 
            ? HttpStatus.TOO_MANY_REQUESTS 
            : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ContestValidationException.class)
    public ResponseEntity<ErrorResponse> handleContestValidation(
            ContestValidationException ex, WebRequest request) {
        Map<String, String> details = new HashMap<>();
        int index = 0;
        for (String error : ex.getErrors()) {
            details.put("error_" + index++, error);
        }
        
        ErrorResponse response = new ErrorResponse(
            "VALIDATION_FAILED",
            ex.getMessage(),
            getPath(request),
            details
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
            InvalidStatusTransitionException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "INVALID_STATUS_TRANSITION",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Judge0Exception.class)
    public ResponseEntity<ErrorResponse> handleJudge0(
            Judge0Exception ex, WebRequest request) {
        logger.error("Judge0 error: {}", ex.getMessage());
        
        HttpStatus status = ex.isTimeout() ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
        String code = ex.isTimeout() ? "GATEWAY_TIMEOUT" : "SERVICE_UNAVAILABLE";
        
        ErrorResponse response = new ErrorResponse(
            code,
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "BAD_REQUEST",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        ErrorResponse response = new ErrorResponse(
            "BAD_REQUEST",
            ex.getMessage(),
            getPath(request)
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error: ", ex);
        
        ErrorResponse response = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            getPath(request)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String getPath(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}

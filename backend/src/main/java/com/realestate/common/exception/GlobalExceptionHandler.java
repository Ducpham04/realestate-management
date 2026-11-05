package com.realestate.common.exception;

import com.realestate.common.api.ApiResponse;
import com.realestate.common.api.ApiResponse.FieldViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleApp(AppException ex) {
        return build(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
                .toList();
        List<FieldViolation> global = ex.getBindingResult().getGlobalErrors().stream()
                .map(ge -> new FieldViolation(ge.getObjectName(), ge.getDefaultMessage()))
                .toList();
        List<FieldViolation> all = new java.util.ArrayList<>(errors);
        all.addAll(global);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(), "Validation failed", all));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        List<FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(lastNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(), "Validation failed", errors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException ex) {
        List<FieldViolation> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new FieldViolation(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED.name(), "Validation failed", errors));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            PropertyReferenceException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        String message = switch (ex) {
            case HttpMessageNotReadableException e -> "Malformed request body";
            case MethodArgumentTypeMismatchException e -> "Invalid value for parameter '%s'".formatted(e.getName());
            case PropertyReferenceException e -> "Invalid sort property '%s'".formatted(e.getPropertyName());
            default -> ex.getMessage();
        };
        return build(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return build(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabled(DisabledException ex) {
        return build(ErrorCode.ACCOUNT_DISABLED, "Account is disabled");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.FORBIDDEN, "You do not have permission to perform this action");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.CONFLICT, "The operation conflicts with existing data");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.name(), ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.RESOURCE_NOT_FOUND, "Endpoint not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return build(ErrorCode.INTERNAL_ERROR, "An unexpected error occurred");
    }

    private static String lastNode(String path) {
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(ApiResponse.error(code.name(), message));
    }
}

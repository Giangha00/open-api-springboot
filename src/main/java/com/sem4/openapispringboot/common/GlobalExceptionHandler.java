package com.sem4.openapispringboot.common;

import com.sem4.openapispringboot.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERROR_BASE = "https://api.example.com/errors/";
    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProductNotFound(ProductNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "product-not-found"));
        pd.setTitle("Product not found");
        pd.setProperty("code", "PRODUCT_NOT_FOUND");
        pd.setProperty("productId", ex.getId());
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "order-not-found"));
        pd.setTitle("Order not found");
        pd.setProperty("code", "ORDER_NOT_FOUND");
        pd.setProperty("orderCode", ex.getCode());
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidTransition(InvalidTransitionException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "invalid-transition"));
        pd.setTitle("Order invalid transition");
        pd.setProperty("code", "ORDER_INVALID_TRANSITION");
        pd.setProperty("orderCode", ex.getOrderCode());
        pd.setProperty("fromStatus", ex.getFromStatus());
        pd.setProperty("action", ex.getAction());
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ProblemDetail> handleIdempotencyConflict(IdempotencyConflictException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "idempotency-conflict"));
        pd.setTitle("Idempotency conflict");
        pd.setProperty("code", "IDEMPOTENCY_CONFLICT");
        pd.setProperty("idempotencyKey", ex.getIdempotencyKey());
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.CONFLICT).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(PreconditionFailedException.class)
    public ResponseEntity<ProblemDetail> handlePreconditionFailed(PreconditionFailedException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "precondition-failed"));
        pd.setTitle("Precondition failed");
        pd.setProperty("code", "PRECONDITION_FAILED");
        pd.setProperty("expectedETag", ex.getExpectedETag());
        pd.setProperty("currentETag", ex.getCurrentETag());
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ProblemDetail> handleBusinessValidation(BusinessValidationException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "business-validation"));
        pd.setTitle("Business validation error");
        pd.setProperty("code", ex.getCode());
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(SecurityException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "forbidden"));
        pd.setTitle("Access denied");
        pd.setProperty("code", "FORBIDDEN");
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(PROBLEM_JSON).body(pd);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "bad-request"));
        pd.setTitle("Bad Request");
        pd.setProperty("code", "BAD_REQUEST");
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(PROBLEM_JSON).body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers,
        HttpStatusCode status, WebRequest request
    ) {
        ProblemDetail pd = ex.getBody();
        pd.setType(URI.create(ERROR_BASE + "validation"));
        pd.setTitle("Validation failed");
        pd.setProperty("code", "VALIDATION_FAILED");
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of(
                "field", fe.getField(),
                "message", String.valueOf(fe.getDefaultMessage())
            ))
            .toList();
        pd.setProperty("errors", fieldErrors);

        return handleExceptionInternal(ex, pd, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setType(URI.create(ERROR_BASE + "internal-error"));
        pd.setTitle("Internal Server Error");
        pd.setProperty("code", "INTERNAL_SERVER_ERROR");
        pd.setProperty("timestamp", Instant.now());
        extractInstance(pd, request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(PROBLEM_JSON).body(pd);
    }

    private void extractInstance(ProblemDetail pd, WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            pd.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
    }
}

package com.assignment.reposcorer.exception;

import com.assignment.reposcorer.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Order(1)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClientError(WebClientResponseException ex, ServerWebExchange exchange) {
        log.warn("WebClient error: {} -> {}. Body: {}", ex.getStatusCode(), ex.getMessage(), ex.getResponseBodyAsString());
        ErrorResponse error = new ErrorResponse(
                OffsetDateTime.now(),
                ex.getStatusCode().value(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        log.warn("Constraint violation: {}", ex.getMessage());
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .findFirst()
                .orElse("Invalid request");
        ErrorResponse error = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                message,
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex, ServerWebExchange exchange) {
        log.warn("Bad request: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> handleWebInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        log.warn("Invalid request parameter: {}", ex.getReason());
        ErrorResponse error = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request parameter: " + ex.getReason(),
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralError(Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Unexpected error: " + ex.getMessage(),
                exchange.getRequest().getPath().value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

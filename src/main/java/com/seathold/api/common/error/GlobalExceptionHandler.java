package com.seathold.api.common.error;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.seathold.api.common.exception.BadRequestException;
import com.seathold.api.common.exception.ConflictException;
import com.seathold.api.common.exception.NotFoundException;
import com.seathold.api.common.response.ApiResponse;
import com.seathold.api.common.response.ApiResponseFactory;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<ApiError>> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ApiError>> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
    }

    private ResponseEntity<ApiResponse<ApiError>> buildErrorResponse(
            HttpStatus status,
            Exception ex,
            HttpServletRequest request) {
        ApiError apiError = new ApiError(
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI(),
                Instant.now());
        return ApiResponseFactory.errorResponse(apiError, status);
    }
}

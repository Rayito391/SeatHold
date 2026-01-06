package com.seathold.api.common.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.seathold.api.common.error.ApiError;

public final class ApiResponseFactory {
    private ApiResponseFactory() {
        throw new IllegalStateException("Utility class");
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, HttpStatus.OK);
    }

    public static <T> ApiResponse<T> success(T data, HttpStatus status) {
        return new ApiResponse<>(
                new ResponseMeta("Success", status.value()),
                data);
    }

    public static ApiResponse<ApiError> error(ApiError apiError, HttpStatus status) {
        return new ApiResponse<>(
                new ResponseMeta("Error", status.value()),
                apiError);
    }

    public static <T> ResponseEntity<ApiResponse<T>> successResponse(T data) {
        return successResponse(data, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> successResponse(T data, HttpStatus status) {
        return ResponseEntity.status(status).body(success(data, status));
    }

    public static ResponseEntity<ApiResponse<ApiError>> errorResponse(ApiError apiError, HttpStatus status) {
        return ResponseEntity.status(status).body(error(apiError, status));
    }
}

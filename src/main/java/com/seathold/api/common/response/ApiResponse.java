package com.seathold.api.common.response;

public record ApiResponse<T>(
                ResponseMeta meta,
                T data) {
}

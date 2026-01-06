package com.seathold.api.common.error;

import java.time.Instant;

public record ApiError(
                String error,
                String message,
                String path,
                Instant timestamp) {
}

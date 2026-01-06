package com.seathold.api.common.constants;

import java.util.UUID;

public final class RedisKeys {
    private RedisKeys() {
    }

    public static String eventAvailable(UUID eventId) {
        return "event:" + eventId + ":available";
    }

    public static String eventLock(UUID eventId) {
        return "lock:event:" + eventId;
    }

    public static String hold(UUID reservationId) {
        return "hold:" + reservationId;
    }

    public static String rateLimitUserMinute(UUID userId, String minuteKey) {
        return "rl:user:" + userId + ":" + minuteKey;
    }
}

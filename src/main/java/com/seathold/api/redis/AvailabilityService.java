package com.seathold.api.redis;

import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.seathold.api.common.constants.RedisKeys;

@Service
public class AvailabilityService {
    private final StringRedisTemplate redis;

    public AvailabilityService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void init(UUID eventId, int totalCapacity) {
        redis.opsForValue().set(RedisKeys.eventAvailable(eventId), String.valueOf(totalCapacity));
    }

    public Integer getAvailable(UUID eventId) {
        String v = redis.opsForValue().get(RedisKeys.eventAvailable(eventId));
        return v == null ? null : Integer.valueOf(v);
    }

    public long decrBy(UUID eventId, int quantity) {
        Long v = redis.opsForValue().increment(RedisKeys.eventAvailable(eventId), -quantity);
        return v == null ? Long.MIN_VALUE : v;
    }

    public long incrBy(UUID eventId, int quantity) {
        Long v = redis.opsForValue().increment(RedisKeys.eventAvailable(eventId), quantity);
        return v == null ? Long.MIN_VALUE : v;
    }

}

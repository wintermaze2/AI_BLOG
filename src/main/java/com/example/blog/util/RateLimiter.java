package com.example.blog.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 키(보통 클라이언트 IP)별 요청 속도 제한. 토큰 버킷 방식.
 *
 * <p>버킷에는 토큰이 {@code capacity} 개까지 담기고 시간이 지나면 일정 속도로 다시 찬다.
 * 요청 한 번에 토큰 하나를 쓰며, 없으면 거절한다. 이 방식은
 * "평소엔 잠깐 몰아서 써도 되지만 지속적으로 많이 쓰지는 못한다"를 자연스럽게 표현한다.
 * 고정 구간 카운터와 달리 구간 경계에서 두 배가 통과하는 문제도 없다.
 *
 * <p><b>메모리</b>: 키마다 버킷을 들고 있으므로 무한정 늘면 그 자체가 취약점이 된다.
 * 그래서 {@code maxEntries} 를 넘으면 오래 쉰 항목을 먼저 지우고,
 * 그래도 넘치면 새 키는 추적하지 않고 통과시킨다(fail-open).
 * 이미 추적 중인 키의 제한은 그대로 유지되므로 진행 중인 남용은 계속 막힌다.
 *
 * <p>스레드 안전하다. 키별 갱신은 {@link ConcurrentHashMap#compute} 안에서 원자적으로 이뤄진다.
 */
public final class RateLimiter {

    private static final class Bucket {
        double tokens;
        long lastRefillMs;
        Bucket(double tokens, long nowMs) { this.tokens = tokens; this.lastRefillMs = nowMs; }
    }

    private final double capacity;
    private final double refillPerMs;
    private final int maxEntries;
    private final long idleEvictMs;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * @param capacity      한 번에 몰아서 쓸 수 있는 최대 요청 수(버스트)
     * @param refillPerMin  분당 회복량 = 지속 가능한 분당 요청 수
     * @param maxEntries    동시에 추적할 최대 키 수(메모리 상한)
     * @param idleEvictMs   이 시간 이상 쉰 키는 정리 대상
     */
    public RateLimiter(double capacity, double refillPerMin, int maxEntries, long idleEvictMs) {
        if (capacity < 1 || refillPerMin <= 0) {
            throw new IllegalArgumentException("capacity >= 1, refillPerMin > 0 이어야 합니다");
        }
        this.capacity = capacity;
        this.refillPerMs = refillPerMin / 60_000.0;
        this.maxEntries = maxEntries;
        this.idleEvictMs = idleEvictMs;
    }

    /**
     * 토큰 하나를 시도한다.
     *
     * @return 0 이면 허용. 양수면 거절이며 값은 다시 시도해도 되는 시점까지의 밀리초.
     */
    public long acquireOrWaitMs(String key, long nowMs) {
        if (!buckets.containsKey(key) && buckets.size() >= maxEntries) {
            evictIdle(nowMs);
            if (buckets.size() >= maxEntries) {
                return 0;   // 추적 여력 없음 -> 통과시킨다(메모리 우선)
            }
        }

        long[] waitMs = {0};
        buckets.compute(key, (k, bucket) -> {
            if (bucket == null) bucket = new Bucket(capacity, nowMs);
            refill(bucket, nowMs);
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                waitMs[0] = 0;
            } else {
                waitMs[0] = (long) Math.ceil((1.0 - bucket.tokens) / refillPerMs);
            }
            return bucket;
        });
        return waitMs[0];
    }

    private void refill(Bucket bucket, long nowMs) {
        long elapsed = nowMs - bucket.lastRefillMs;
        if (elapsed <= 0) return;                    // 시계 역행 방어
        bucket.tokens = Math.min(capacity, bucket.tokens + elapsed * refillPerMs);
        bucket.lastRefillMs = nowMs;
    }

    private void evictIdle(long nowMs) {
        buckets.entrySet().removeIf(e -> nowMs - e.getValue().lastRefillMs > idleEvictMs);
    }

    /** 현재 추적 중인 키 수(모니터링용). */
    public int trackedKeys() {
        return buckets.size();
    }
}

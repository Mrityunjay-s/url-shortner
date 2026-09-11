package com.personal.urlshort.constants;

import java.time.Duration;

public class RedisConstants {

    private RedisConstants() {
    }

    public static final String URL_COUNTER = "shortener:counter";
    public static final String URL_PREFIX = "shortener:url:";
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
}

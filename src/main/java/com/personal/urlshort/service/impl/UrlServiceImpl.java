package com.personal.urlshort.service.impl;

import com.personal.urlshort.constants.RedisConstants;
import com.personal.urlshort.dto.UrlRequestDto;
import com.personal.urlshort.dto.UrlResponseDto;
import com.personal.urlshort.entities.Urls;
import com.personal.urlshort.exceptions.UrlNotFound;
import com.personal.urlshort.repository.UrlRepository;
import com.personal.urlshort.service.UrlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public UrlServiceImpl(UrlRepository urlRepository, StringRedisTemplate stringRedisTemplate) {
        this.urlRepository = urlRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional
    public UrlResponseDto shorten(UrlRequestDto urlRequestDto) {
        /*
         * Check whether a short URL already exists for the given long URL.
         * If it exists, return the existing short URL; otherwise, generate a new one.
         */
        Optional<Urls> url = urlRepository.findByLongUrl(urlRequestDto.getUrl());

        if (url.isPresent()) {
            log.info("Short URL already exists for long URL: {}", urlRequestDto.getUrl());
            return new UrlResponseDto(url.get().getShortCode());
        }
        log.info("No short URL exists for long URL: {}", urlRequestDto.getUrl());

        // Generate code using current counter
        Long counter = getCounter();

        String encoded = Base64.getEncoder()
                .encodeToString(counter.toString().getBytes(StandardCharsets.UTF_8));

        Urls saved = urlRepository.save(
                Urls.builder()
                        .shortCode(encoded)
                        .longUrl(urlRequestDto.getUrl())
                        .build()
        );

        return new UrlResponseDto(saved.getShortCode());
    }

    @Override
    public UrlResponseDto getLongUrl(String code) {
        String longUrl = stringRedisTemplate.opsForValue().get(RedisConstants.URL_PREFIX + code);

        // Cache hit
        if (longUrl != null) {
            log.info("Short URL found in Redis: {}", code);
            return new UrlResponseDto(longUrl);
        }

        // Cache miss → DB
        Urls url = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFound("Short URL not found"));

        // Store in Redis for future requests
        stringRedisTemplate.opsForValue()
                .set(RedisConstants.URL_PREFIX + code, url.getLongUrl());

        log.info("Short URL found in DB and cached in Redis: {}", code);

        return new UrlResponseDto(url.getLongUrl());
    }

    @Override
    @Transactional
    public void decativateUrl(String code) {

        String key = RedisConstants.URL_PREFIX + code;

        // Remove from Redis
        Boolean deleted = stringRedisTemplate.delete(key);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("Short URL removed from Redis: {}", code);
        } else {
            log.info("Short URL not found in Redis: {}", code);
        }

        int deletedRows = urlRepository.deleteByShortCode(code);

        if (deletedRows == 0) {
            throw new UrlNotFound("Short URL not found: " + code);
        }
        log.info("Short URL deactivated: {}", code);
    }

    private Long getCounter() {
        return stringRedisTemplate.opsForValue().increment(RedisConstants.URL_COUNTER);
    }

}

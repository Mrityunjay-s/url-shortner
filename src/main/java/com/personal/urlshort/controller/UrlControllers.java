package com.personal.urlshort.controller;

import com.personal.urlshort.dto.UrlRequestDto;
import com.personal.urlshort.dto.UrlResponseDto;
import com.personal.urlshort.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController()
public class UrlControllers {
    private final UrlService urlService;

    public UrlControllers(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<UrlResponseDto> shortUrl(@RequestBody @Valid UrlRequestDto urlRequestDto) {
        UrlResponseDto response = urlService.shorten(urlRequestDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<UrlResponseDto> getShortUrl(@PathVariable String code) {
        UrlResponseDto longUrl = urlService.getLongUrl(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(longUrl.getUrl()))
                .build();
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> deactivateUrl(@PathVariable String code) {
        urlService.decativateUrl(code);
        return ResponseEntity.noContent().build();
    }
}

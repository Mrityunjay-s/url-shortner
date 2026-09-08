package com.personal.urlshort.service;

import com.personal.urlshort.dto.UrlRequestDto;
import com.personal.urlshort.dto.UrlResponseDto;
import jakarta.validation.Valid;

public interface UrlService {
    UrlResponseDto shorten(UrlRequestDto urlRequestDto);
    UrlResponseDto getLongUrl(String urlRequestDto);
    void decativateUrl(String url);
}

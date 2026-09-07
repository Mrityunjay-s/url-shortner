package com.personal.urlshort.service;

import com.personal.urlshort.dto.UrlRequestDto;
import com.personal.urlshort.dto.UrlResponseDto;

public interface UrlService {
    UrlResponseDto shorten(UrlRequestDto urlRequestDto);
    UrlResponseDto getLongUrl(String urlRequestDto);
}

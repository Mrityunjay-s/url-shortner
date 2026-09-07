package com.personal.urlshort.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UrlRequestDto {

    @NotBlank
    private String url;
}

package com.personal.urlshort.exceptions;

public class UrlNotFound extends RuntimeException {
    public UrlNotFound(String message) {
        super(message);
    }

    public UrlNotFound() {
        super();
    }

    public UrlNotFound(String message, Throwable cause) {
        super(message, cause);
    }
}

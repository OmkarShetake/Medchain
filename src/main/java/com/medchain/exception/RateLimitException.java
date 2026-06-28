package com.medchain.exception;

public class RateLimitException extends RuntimeException {
    private final long retryAfter;
    
    public RateLimitException(String message, long retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }
    
    public long getRetryAfter() {
        return retryAfter;
    }
}

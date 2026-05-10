package com.thinkai4j.core.exception;

public class AiException extends RuntimeException {

    private String provider;
    private String errorCode;

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiException(String provider, String errorCode, String message) {
        super(message);
        this.provider = provider;
        this.errorCode = errorCode;
    }

    public AiException(String provider, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = errorCode;
    }

    public String getProvider() {
        return provider;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

package com.thinkai4j.core.retry;

import com.thinkai4j.core.exception.AiException;

import java.util.function.Supplier;

public class RetryHandler {

    private int maxAttempts = 3;
    private long initialDelayMs = 1000;
    private double backoffMultiplier = 2.0;
    private java.util.Set<Class<? extends Throwable>> retryableExceptions = new java.util.HashSet<>();

    public RetryHandler() {
        retryableExceptions.add(Exception.class);
    }

    public static RetryHandler defaults() {
        return new RetryHandler();
    }

    public RetryHandler maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public RetryHandler initialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
        return this;
    }

    public RetryHandler backoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
        return this;
    }

    public RetryHandler retryOn(Class<? extends Throwable> exceptionClass) {
        retryableExceptions.add(exceptionClass);
        return this;
    }

    public <T> T execute(Supplier<T> supplier) {
        long delayMs = initialDelayMs;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt == maxAttempts || !isRetryable(e)) {
                    if (e instanceof AiException) {
                        throw (AiException) e;
                    }
                    throw new AiException("Operation failed after " + attempt + " attempts", e);
                }
                sleep(delayMs);
                delayMs = (long) (delayMs * backoffMultiplier);
            }
        }

        throw new AiException("Operation failed after " + maxAttempts + " attempts", lastException);
    }

    private boolean isRetryable(Exception e) {
        for (Class<? extends Throwable> clazz : retryableExceptions) {
            if (clazz.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

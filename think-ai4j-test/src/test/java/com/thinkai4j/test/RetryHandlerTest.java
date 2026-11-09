package com.thinkai4j.test;

import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.retry.RetryHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 重试语义回归：业务异常（含 4xx 的 AiException）不得重试，IO 异常按配置重试。
 */
class RetryHandlerTest {

    @Test
    void businessExceptionIsNotRetried() {
        AtomicInteger attempts = new AtomicInteger();
        RetryHandler handler = RetryHandler.defaults().initialDelayMs(1);

        assertThrows(AiException.class, () -> handler.execute(() -> {
            attempts.incrementAndGet();
            throw new AiException("client error");
        }));
        assertEquals(1, attempts.get(), "业务异常不应触发重试");
    }

    @Test
    void ioExceptionIsRetriedUpToMaxAttempts() {
        AtomicInteger attempts = new AtomicInteger();
        RetryHandler handler = RetryHandler.defaults().maxAttempts(3).initialDelayMs(1);

        // Supplier 不允许抛受检异常，用非受检异常包装；RetryHandler 会沿 cause 链识别 IOException
        assertThrows(AiException.class, () -> handler.execute(() -> {
            attempts.incrementAndGet();
            throw new java.io.UncheckedIOException(new IOException("network failure"));
        }));
        assertEquals(3, attempts.get(), "IO 异常应按最大次数重试");
    }
}

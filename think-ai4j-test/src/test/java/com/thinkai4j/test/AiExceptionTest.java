package com.thinkai4j.test;

import com.thinkai4j.core.exception.AiException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiExceptionTest {

    @Test
    void testMessageOnlyConstructor() {
        AiException ex = new AiException("Test error");
        assertEquals("Test error", ex.getMessage());
        assertNull(ex.getProvider());
        assertNull(ex.getErrorCode());
    }

    @Test
    void testFullConstructor() {
        AiException ex = new AiException("openai", "API_ERROR", "Rate limited");
        assertEquals("openai", ex.getProvider());
        assertEquals("API_ERROR", ex.getErrorCode());
        assertEquals("Rate limited", ex.getMessage());
    }

    @Test
    void testCauseConstructor() {
        RuntimeException cause = new RuntimeException("connection refused");
        AiException ex = new AiException("openai", "NETWORK_ERROR", "Failed to connect", cause);
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testNullFields() {
        AiException ex = new AiException(null, null, null);
        assertNull(ex.getProvider());
        assertNull(ex.getErrorCode());
    }
}

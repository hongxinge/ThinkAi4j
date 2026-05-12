package com.thinkai4j.test;

import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatProviderRegistryTest {

    private ChatProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ChatProviderRegistry();
        registry.registerProvider(new TestProvider());
    }

    @Test
    void testRegisterProvider() {
        assertTrue(registry.hasProvider("test-provider"));
    }

    @Test
    void testGetProvider() {
        ChatProvider provider = registry.getProvider("test-provider");
        assertNotNull(provider);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testGetProviderReturnsNullForUnknownProvider() {
        assertNull(registry.getProvider("unknown"));
    }

    @Test
    void testGetAvailableProviders() {
        registry.registerProvider(new TestProvider("another"));
        assertEquals(2, registry.getAvailableProviders().size());
    }

    @Test
    void testDefaultProviderNotSet() {
        assertNull(registry.getDefaultProvider());
        assertThrows(AiException.class, () -> registry.getDefaultProviderInstance());
    }

    @Test
    void testDefaultProviderSet() {
        registry.setDefaultProvider("test-provider");
        assertEquals("test-provider", registry.getDefaultProvider());
        assertNotNull(registry.getDefaultProviderInstance());
    }

    @Test
    void testDefaultProviderNotInRegistry() {
        registry.setDefaultProvider("non-existent");
        assertThrows(AiException.class, () -> registry.getDefaultProviderInstance());
    }

    private static class TestProvider implements ChatProvider {
        private final String name;

        TestProvider() {
            this("test-provider");
        }

        TestProvider(String name) {
            this.name = name;
        }

        @Override
        public String getProviderName() {
            return name;
        }

        @Override
        public AiResponse chat(ChatRequest request) {
            return new AiResponse("test response");
        }

        @Override
        public reactor.core.publisher.Flux<String> stream(ChatRequest request) {
            return reactor.core.publisher.Flux.just("test", "stream");
        }
    }
}

package com.thinkai4j.spring.boot.starter;

import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.api.DefaultAiChat;
import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.memory.InMemoryChatMemory;
import com.thinkai4j.memory.redis.RedisChatMemory;
import com.thinkai4j.provider.compat.OpenAiCompatConfig;
import com.thinkai4j.provider.compat.OpenAiCompatProvider;
import com.thinkai4j.rag.DocumentStore;
import com.thinkai4j.rag.InMemoryDocumentStore;
import com.thinkai4j.rag.RagPipeline;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@Configuration
@EnableConfigurationProperties(ThinkAiProperties.class)
public class ThinkAiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ChatProviderRegistry chatProviderRegistry(ThinkAiProperties properties) {
        ChatProviderRegistry registry = new ChatProviderRegistry();
        registry.setDefaultProvider(properties.getDefaultProvider());
        return registry;
    }

    @Configuration
    @ConditionalOnClass(OpenAiCompatProvider.class)
    @ConditionalOnProperty(prefix = "think.ai.compat", name = "providers")
    static class OpenAiCompatAutoConfiguration {

        @Bean
        public List<ChatProvider> openAiCompatProviders(OpenAiCompatConfig config) {
            return config.getProviders().stream()
                .<ChatProvider>map(providerConfig -> new OpenAiCompatProvider(providerConfig, config.getHttpClient()))
                .toList();
        }

        @Bean
        public ChatProvider openAiCompatRegistrar(List<ChatProvider> providers, ChatProviderRegistry registry) {
            for (ChatProvider provider : providers) {
                registry.registerProvider(provider);
                if (registry.getDefaultProvider() == null) {
                    registry.setDefaultProvider(provider.getProviderName());
                }
            }
            return providers.isEmpty() ? null : providers.get(0);
        }
    }

    @Bean
    @ConditionalOnMissingBean(AiChat.class)
    public AiChat aiChat(ChatProviderRegistry registry, ChatMemory chatMemory) {
        return new DefaultAiChat(registry, chatMemory);
    }

    @Configuration
    @ConditionalOnClass(RedisChatMemory.class)
    @ConditionalOnProperty(prefix = "think.ai.memory", name = "type", havingValue = "redis")
    static class RedisMemoryAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatMemory.class)
        public ChatMemory redisChatMemory(StringRedisTemplate redisTemplate, ThinkAiProperties properties) {
            RedisChatMemory memory = new RedisChatMemory(redisTemplate);
            if (properties.getMemory() != null && properties.getMemory().getMaxMessages() != null) {
                memory.setMaxMessages(properties.getMemory().getMaxMessages());
            } else {
                memory.setMaxMessages(20);
            }
            if (properties.getMemory() != null && properties.getMemory().getTtlMinutes() != null) {
                memory.setTtlMinutes(properties.getMemory().getTtlMinutes());
            } else {
                memory.setTtlMinutes(60);
            }
            return memory;
        }
    }

    @Configuration
    @ConditionalOnClass(ChatMemory.class)
    @ConditionalOnProperty(prefix = "think.ai.memory", name = "type", havingValue = "memory", matchIfMissing = true)
    static class InMemoryAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatMemory.class)
        public ChatMemory chatMemory(ThinkAiProperties properties) {
            InMemoryChatMemory memory = new InMemoryChatMemory();
            if (properties.getMemory() != null && properties.getMemory().getMaxMessages() != null) {
                memory.setMaxMessages(properties.getMemory().getMaxMessages());
            } else {
                memory.setMaxMessages(20);
            }
            return memory;
        }
    }

    @Configuration
    @ConditionalOnClass(RagPipeline.class)
    static class RagAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(DocumentStore.class)
        public DocumentStore documentStore() {
            return new InMemoryDocumentStore();
        }

        @Bean
        @ConditionalOnMissingBean(RagPipeline.class)
        public RagPipeline ragPipeline(AiChat chat, DocumentStore documentStore) {
            return new RagPipeline(chat, documentStore);
        }
    }
}

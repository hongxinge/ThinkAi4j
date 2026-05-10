package com.thinkai4j.memory.redis;

import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.core.model.AiMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RedisChatMemory implements ChatMemory {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private int maxMessages = 20;
    private long ttlMinutes = 60;
    private final String keyPrefix = "thinkai4j:memory:";

    public RedisChatMemory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void addMessage(String conversationId, AiMessage message) {
        String key = keyPrefix + conversationId;
        List<AiMessage> messages = getMessages(conversationId);
        messages.add(message);

        if (messages.size() > maxMessages) {
            messages = messages.subList(messages.size() - maxMessages, messages.size());
        }

        try {
            String json = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, json, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save message to Redis", e);
        }
    }

    @Override
    public List<AiMessage> getMessages(String conversationId) {
        String key = keyPrefix + conversationId;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return new ArrayList<>();
        }

        try {
            List<AiMessage> messages = objectMapper.readValue(json, new TypeReference<List<AiMessage>>() {});
            return messages != null ? Collections.unmodifiableList(messages) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public void clear(String conversationId) {
        String key = keyPrefix + conversationId;
        redisTemplate.delete(key);
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public void setTtlMinutes(long ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }
}

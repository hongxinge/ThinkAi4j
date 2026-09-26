package com.thinkai4j.memory.redis;

import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.core.model.AiMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 基于 Redis 的对话记忆存储。
 *
 * <p>使用 Redis List 结构存储消息：追加消息通过 RPUSH 原子完成，
 * 配合 Lua 脚本在同一原子操作内完成裁剪（LTRIM）与续期（EXPIRE），
 * 避免旧实现"读取-修改-整体回写"在并发场景下互相覆盖丢消息的问题。</p>
 */
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "thinkai4j:memory:";

    /**
     * 原子追加并裁剪、续期：
     * KEYS[1] = 记忆 key；ARGV[1] = 消息 JSON；ARGV[2] = 最大条数；ARGV[3] = TTL 秒
     */
    private static final DefaultRedisScript<Long> APPEND_SCRIPT = new DefaultRedisScript<>(
            "redis.call('RPUSH', KEYS[1], ARGV[1]) " +
            "redis.call('LTRIM', KEYS[1], -tonumber(ARGV[2]), -1) " +
            "redis.call('EXPIRE', KEYS[1], ARGV[3]) " +
            "return 1",
            Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private int maxMessages = 20;
    private long ttlMinutes = 60;

    public RedisChatMemory(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void addMessage(String conversationId, AiMessage message) {
        if (conversationId == null || conversationId.isEmpty() || message == null) {
            return;
        }
        String key = KEY_PREFIX + conversationId;
        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.execute(APPEND_SCRIPT, Collections.singletonList(key),
                    json, String.valueOf(maxMessages), String.valueOf(ttlMinutes * 60));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save message to Redis", e);
        }
    }

    @Override
    public List<AiMessage> getMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return new ArrayList<>();
        }
        String key = KEY_PREFIX + conversationId;
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonList == null || jsonList.isEmpty()) {
            return new ArrayList<>();
        }

        List<AiMessage> messages = new ArrayList<>(jsonList.size());
        for (String json : jsonList) {
            try {
                AiMessage message = objectMapper.readValue(json, AiMessage.class);
                if (message != null) {
                    messages.add(message);
                }
            } catch (Exception e) {
                // 单条消息损坏时跳过，不影响其余历史消息读取
            }
        }
        return Collections.unmodifiableList(messages);
    }

    @Override
    public void clear(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return;
        }
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }

    public void setMaxMessages(int maxMessages) {
        if (maxMessages > 0) {
            this.maxMessages = maxMessages;
        }
    }

    public void setTtlMinutes(long ttlMinutes) {
        if (ttlMinutes > 0) {
            this.ttlMinutes = ttlMinutes;
        }
    }
}

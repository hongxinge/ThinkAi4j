package com.thinkai4j.memory;

import com.thinkai4j.core.model.AiMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryChatMemory implements ChatMemoryStore {

    private final Map<String, List<AiMessage>> store = new ConcurrentHashMap<>();
    private int maxMessages = 20;

    @Override
    public void addMessage(String conversationId, AiMessage message) {
        store.computeIfAbsent(conversationId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(message);
        trimMessages(conversationId);
    }

    @Override
    public List<AiMessage> getMessages(String conversationId) {
        List<AiMessage> messages = store.get(conversationId);
        if (messages == null) {
            return Collections.emptyList();
        }
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }

    @Override
    public void clear(String conversationId) {
        store.remove(conversationId);
    }

    @Override
    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    private void trimMessages(String conversationId) {
        List<AiMessage> messages = store.get(conversationId);
        if (messages == null) {
            return;
        }
        synchronized (messages) {
            while (messages.size() > maxMessages) {
                messages.remove(0);
            }
        }
    }
}

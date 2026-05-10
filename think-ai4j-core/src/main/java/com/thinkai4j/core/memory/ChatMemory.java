package com.thinkai4j.core.memory;

import java.util.List;

public interface ChatMemory {

    void addMessage(String conversationId, com.thinkai4j.core.model.AiMessage message);

    List<com.thinkai4j.core.model.AiMessage> getMessages(String conversationId);

    void clear(String conversationId);
}

package com.thinkai4j.memory;

import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.core.model.AiMessage;
import java.util.List;

public interface ChatMemoryStore extends ChatMemory {

    void setMaxMessages(int maxMessages);
}

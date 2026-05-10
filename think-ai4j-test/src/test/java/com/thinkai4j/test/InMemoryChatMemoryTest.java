package com.thinkai4j.test;

import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.memory.InMemoryChatMemory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryChatMemoryTest {

    @Test
    void testAddAndGetMessages() {
        InMemoryChatMemory memory = new InMemoryChatMemory();
        memory.addMessage("user-1", AiMessage.user("Hello"));
        memory.addMessage("user-1", AiMessage.assistant("Hi!"));

        List<AiMessage> messages = memory.getMessages("user-1");
        assertEquals(2, messages.size());
        assertEquals("Hello", messages.get(0).getContent());
        assertEquals("Hi!", messages.get(1).getContent());
    }

    @Test
    void testMaxMessagesLimit() {
        InMemoryChatMemory memory = new InMemoryChatMemory();
        memory.setMaxMessages(3);

        memory.addMessage("user-1", AiMessage.user("1"));
        memory.addMessage("user-1", AiMessage.assistant("2"));
        memory.addMessage("user-1", AiMessage.user("3"));
        memory.addMessage("user-1", AiMessage.assistant("4"));

        List<AiMessage> messages = memory.getMessages("user-1");
        assertEquals(3, messages.size());
        assertEquals("2", messages.get(0).getContent());
        assertEquals("3", messages.get(1).getContent());
        assertEquals("4", messages.get(2).getContent());
    }

    @Test
    void testClear() {
        InMemoryChatMemory memory = new InMemoryChatMemory();
        memory.addMessage("user-1", AiMessage.user("Hello"));
        memory.clear("user-1");

        assertTrue(memory.getMessages("user-1").isEmpty());
    }

    @Test
    void testSeparateConversations() {
        InMemoryChatMemory memory = new InMemoryChatMemory();
        memory.addMessage("user-1", AiMessage.user("User 1 msg"));
        memory.addMessage("user-2", AiMessage.user("User 2 msg"));

        assertEquals(1, memory.getMessages("user-1").size());
        assertEquals(1, memory.getMessages("user-2").size());
        assertEquals("User 1 msg", memory.getMessages("user-1").get(0).getContent());
        assertEquals("User 2 msg", memory.getMessages("user-2").get(0).getContent());
    }
}

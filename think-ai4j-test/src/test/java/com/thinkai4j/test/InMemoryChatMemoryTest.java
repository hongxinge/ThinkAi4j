package com.thinkai4j.test;

import com.thinkai4j.memory.InMemoryChatMemory;
import com.thinkai4j.core.model.AiMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryChatMemoryTest {

    private InMemoryChatMemory memory;

    @BeforeEach
    void setUp() {
        memory = new InMemoryChatMemory();
    }

    @Test
    void testAddAndGetMessages() {
        memory.addMessage("user-1", AiMessage.user("Hello"));
        memory.addMessage("user-1", AiMessage.assistant("Hi"));

        List<AiMessage> messages = memory.getMessages("user-1");
        assertEquals(2, messages.size());
    }

    @Test
    void testMaxMessagesTruncation() {
        memory.setMaxMessages(2);
        memory.addMessage("user-1", AiMessage.user("1"));
        memory.addMessage("user-1", AiMessage.user("2"));
        memory.addMessage("user-1", AiMessage.user("3"));

        List<AiMessage> messages = memory.getMessages("user-1");
        assertEquals(2, messages.size());
        assertEquals("2", messages.get(0).getContent());
        assertEquals("3", messages.get(1).getContent());
    }

    @Test
    void testClearMessages() {
        memory.addMessage("user-1", AiMessage.user("Hello"));
        memory.clear("user-1");

        List<AiMessage> messages = memory.getMessages("user-1");
        assertTrue(messages.isEmpty());
    }

    @Test
    void testMultipleConversationIdsAreIsolated() {
        memory.addMessage("user-1", AiMessage.user("Hello from user-1"));
        memory.addMessage("user-2", AiMessage.user("Hello from user-2"));

        assertEquals(1, memory.getMessages("user-1").size());
        assertEquals(1, memory.getMessages("user-2").size());
        assertEquals("Hello from user-1", memory.getMessages("user-1").get(0).getContent());
        assertEquals("Hello from user-2", memory.getMessages("user-2").get(0).getContent());
    }

    @Test
    void testGetMessagesReturnsImmutableList() {
        memory.addMessage("user-1", AiMessage.user("Hello"));
        List<AiMessage> messages = memory.getMessages("user-1");

        assertThrows(UnsupportedOperationException.class, () -> messages.add(AiMessage.user("new")));
    }

    @Test
    void testGetMessagesForUnknownIdReturnsEmpty() {
        List<AiMessage> messages = memory.getMessages("non-existent");
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }
}

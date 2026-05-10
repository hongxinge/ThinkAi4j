package com.thinkai4j.test;

import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiMessageTest {

    @Test
    void testSystemMessage() {
        AiMessage message = AiMessage.system("You are a helpful assistant");
        assertEquals(MessageType.SYSTEM, message.getRole());
        assertEquals("You are a helpful assistant", message.getContent());
    }

    @Test
    void testUserMessage() {
        AiMessage message = AiMessage.user("Hello");
        assertEquals(MessageType.USER, message.getRole());
        assertEquals("Hello", message.getContent());
    }

    @Test
    void testAssistantMessage() {
        AiMessage message = AiMessage.assistant("Hi there!");
        assertEquals(MessageType.ASSISTANT, message.getRole());
        assertEquals("Hi there!", message.getContent());
    }

    @Test
    void testConstructor() {
        AiMessage message = new AiMessage(MessageType.USER, "Test content");
        assertEquals(MessageType.USER, message.getRole());
        assertEquals("Test content", message.getContent());
    }
}

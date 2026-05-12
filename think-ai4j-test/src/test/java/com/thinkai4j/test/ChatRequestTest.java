package com.thinkai4j.test;

import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.ToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatRequestTest {

    @Test
    void testBuilderWithSingleMessage() {
        ChatRequest request = ChatRequest.builder()
                .model("gpt-4")
                .message("Hello")
                .build();

        assertEquals("gpt-4", request.getModel());
        assertEquals(1, request.getMessages().size());
        assertEquals("Hello", request.getMessages().get(0).getContent());
    }

    @Test
    void testBuilderWithSystemAndMessage() {
        ChatRequest request = ChatRequest.builder()
                .system("You are helpful")
                .message("Hi")
                .build();

        assertEquals(2, request.getMessages().size());
    }

    @Test
    void testBuilderMessagesReplacesPreviousMessages() {
        ChatRequest request = ChatRequest.builder()
                .system("You are helpful")
                .message("Hi")
                .messages(List.of(AiMessage.user("override")))
                .build();

        assertEquals(1, request.getMessages().size());
        assertEquals("override", request.getMessages().get(0).getContent());
    }

    @Test
    void testBuilderWithTools() {
        ToolDefinition.FunctionDefinition funcDef = new ToolDefinition.FunctionDefinition();
        funcDef.setName("get_weather");
        funcDef.setDescription("Get weather for a city");

        ToolDefinition tool = new ToolDefinition();
        tool.setFunction(funcDef);

        ChatRequest request = ChatRequest.builder()
                .message("What's the weather in Beijing?")
                .tools(List.of(tool))
                .build();

        assertEquals(1, request.getTools().size());
        assertEquals("get_weather", request.getTools().get(0).getFunction().getName());
    }

    @Test
    void testBuilderWithTemperatureAndMaxTokens() {
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .temperature(0.7)
                .maxTokens(1000)
                .build();

        assertEquals(0.7, request.getTemperature());
        assertEquals(1000, request.getMaxTokens());
    }

    @Test
    void testBuilderWithStream() {
        ChatRequest request = ChatRequest.builder()
                .message("Hello")
                .stream(true)
                .build();

        assertTrue(request.getStream());
    }

    @Test
    void testEmptyConstructorHasEmptyMessages() {
        ChatRequest request = new ChatRequest();
        assertNotNull(request.getMessages());
        assertTrue(request.getMessages().isEmpty());
    }
}

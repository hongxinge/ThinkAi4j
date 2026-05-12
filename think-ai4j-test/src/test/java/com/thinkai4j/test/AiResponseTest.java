package com.thinkai4j.test;

import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.model.Usage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AiResponseTest {

    @Test
    void testEmptyConstructor() {
        AiResponse response = new AiResponse();
        assertNull(response.getContent());
        assertNull(response.getModel());
        assertNull(response.getFinishReason());
        assertNull(response.getToolCalls());
        assertNull(response.getUsage());
    }

    @Test
    void testContentConstructor() {
        AiResponse response = new AiResponse("Hello");
        assertEquals("Hello", response.getContent());
    }

    @Test
    void testSettersAndGetters() {
        AiResponse response = new AiResponse();
        response.setContent("Hello");
        response.setModel("gpt-4");
        response.setFinishReason("stop");

        assertEquals("Hello", response.getContent());
        assertEquals("gpt-4", response.getModel());
        assertEquals("stop", response.getFinishReason());
    }

    @Test
    void testToolCalls() {
        AiResponse response = new AiResponse();
        ToolCall toolCall = new ToolCall();
        toolCall.setId("call_1");
        toolCall.setType("function");
        ToolCall.FunctionCall function = new ToolCall.FunctionCall();
        function.setName("get_weather");
        function.setArguments("{\"city\":\"beijing\"}");
        toolCall.setFunction(function);

        response.setToolCalls(List.of(toolCall));

        assertEquals(1, response.getToolCalls().size());
        assertEquals("call_1", response.getToolCalls().get(0).getId());
        assertEquals("get_weather", response.getToolCalls().get(0).getFunction().getName());
    }

    @Test
    void testUsage() {
        AiResponse response = new AiResponse();
        Usage usage = new Usage(10, 20, 30);
        response.setUsage(usage);

        assertEquals(10, response.getUsage().getPromptTokens());
        assertEquals(20, response.getUsage().getCompletionTokens());
        assertEquals(30, response.getUsage().getTotalTokens());
    }
}

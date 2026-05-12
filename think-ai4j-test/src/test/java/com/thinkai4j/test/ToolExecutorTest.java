package com.thinkai4j.test;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;
import com.thinkai4j.tool.ToolExecutor;
import com.thinkai4j.core.model.ToolCall;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ToolExecutorTest {

    private ToolExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new ToolExecutor();
        executor.register(new WeatherTool());
    }

    @Test
    void testRegisterAndGetToolDefinitions() {
        List<com.thinkai4j.core.model.ToolDefinition> defs = executor.getToolDefinitions();
        assertEquals(1, defs.size());
        assertEquals("getWeather", defs.get(0).getFunction().getName());
    }

    @Test
    void testExecuteToolSuccessfully() {
        ToolCall toolCall = new ToolCall();
        toolCall.setId("call_1");
        toolCall.setType("function");
        ToolCall.FunctionCall function = new ToolCall.FunctionCall();
        function.setName("getWeather");
        function.setArguments("{\"city\":\"beijing\"}");
        toolCall.setFunction(function);

        String result = executor.execute(toolCall);
        assertNotNull(result);
        assertTrue(result.contains("beijing"));
    }

    @Test
    void testExecuteNonExistentTool() {
        ToolCall toolCall = new ToolCall();
        toolCall.setId("call_2");
        toolCall.setType("function");
        ToolCall.FunctionCall function = new ToolCall.FunctionCall();
        function.setName("nonExistent");
        function.setArguments("{}");
        toolCall.setFunction(function);

        assertThrows(com.thinkai4j.core.exception.AiException.class, () -> executor.execute(toolCall));
    }

    @Test
    void testHasTool() {
        assertTrue(executor.hasTool("getWeather"));
        assertFalse(executor.hasTool("nonExistent"));
    }

    public static class WeatherTool {
        @AiTool("查询天气")
        public String getWeather(@ToolParam(description = "城市名称") String city) {
            return city + " 晴天，25度";
        }
    }
}

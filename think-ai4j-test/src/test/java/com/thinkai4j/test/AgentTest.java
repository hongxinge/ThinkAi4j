package com.thinkai4j.test;

import com.thinkai4j.agent.Agent;
import com.thinkai4j.agent.AgentBus;
import com.thinkai4j.agent.AgentLongTermMemory;
import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.model.*;
import com.thinkai4j.core.api.DefaultAiChat;
import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.memory.InMemoryChatMemory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentTest {

    private Agent agent;
    private ChatProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ChatProviderRegistry();
        registry.registerProvider(new MockChatProvider());
        registry.setDefaultProvider("mock");

        AiChat chat = new com.thinkai4j.core.api.DefaultAiChat(registry);
        agent = new Agent("test-agent", "你是专业的助手", chat);
    }

    @Test
    void testAgentName() {
        assertEquals("test-agent", agent.getName());
    }

    @Test
    void testAddToolBean() {
        agent.addToolBean(new MockTool());
        assertTrue(agent.getToolExecutor().hasTool("greet"));
    }

    @Test
    void testMaxIterations() {
        Agent customAgent = new Agent("custom", "test",
                new com.thinkai4j.core.api.DefaultAiChat(registry));
        customAgent.maxIterations(5);
        assertNotNull(customAgent.getToolExecutor());
    }

    @Test
    void testMemoryIntegration() {
        ChatMemory memory = new InMemoryChatMemory();
        agent.memory(memory, "test-conv");

        assertNotNull(agent);
    }

    @Test
    void testLongTermMemoryIntegration() {
        InMemoryChatMemory memory = new InMemoryChatMemory();
        AgentLongTermMemory longTermMemory = new AgentLongTermMemory("agent-1", memory);
        agent.longTermMemory(longTermMemory);

        assertNotNull(agent);
    }

    @Test
    void testExecuteWithMockProvider() {
        String result = agent.execute("Hello");
        assertNotNull(result);
    }

    private static class MockChatProvider implements ChatProvider {
        @Override
        public String getProviderName() {
            return "mock";
        }

        @Override
        public AiResponse chat(ChatRequest request) {
            return new AiResponse("Mock response for: " +
                    request.getMessages().get(request.getMessages().size() - 1).getContent());
        }

        @Override
        public reactor.core.publisher.Flux<String> stream(ChatRequest request) {
            return reactor.core.publisher.Flux.just("mock", "stream");
        }
    }

    public static class MockTool {
        @com.thinkai4j.tool.annotation.AiTool("打招呼")
        public String greet(@com.thinkai4j.tool.annotation.ToolParam(description = "名字") String name) {
            return "你好, " + name + "!";
        }
    }
}

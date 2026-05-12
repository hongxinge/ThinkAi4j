package com.thinkai4j.test;

import com.thinkai4j.agent.Agent;
import com.thinkai4j.agent.AgentBus;
import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AgentBusTest {

    private AgentBus bus;

    @BeforeEach
    void setUp() {
        bus = new AgentBus();
    }

    @Test
    void testRegisterAgent() {
        Agent agent = createMockAgent("test-agent");
        bus.register("test-agent", agent);

        assertTrue(bus.getAgentNames().contains("test-agent"));
        assertEquals(agent, bus.getAgent("test-agent"));
    }

    @Test
    void testUnregisterAgent() {
        Agent agent = createMockAgent("test-agent");
        bus.register("test-agent", agent);
        bus.unregister("test-agent");

        assertFalse(bus.getAgentNames().contains("test-agent"));
        assertNull(bus.getAgent("test-agent"));
    }

    @Test
    void testExecute() {
        bus.register("assistant", createMockAgent("assistant"));
        String result = bus.execute("assistant", "Hello");

        assertNotNull(result);
    }

    @Test
    void testExecuteNonExistentAgent() {
        String result = bus.execute("non-existent", "Hello");
        assertEquals("Agent not found: non-existent", result);
    }

    @Test
    void testChainExecute() {
        bus.register("researcher", createMockAgent("researcher"));
        bus.register("writer", createMockAgent("writer"));

        String result = bus.chainExecute(List.of("researcher", "writer"), "研究并写报告");
        assertNotNull(result);
    }

    @Test
    void testParallelExecute() {
        bus.register("agent1", createMockAgent("agent1"));
        bus.register("agent2", createMockAgent("agent2"));

        String result = bus.parallelExecute(Map.of(
                "agent1", "任务1",
                "agent2", "任务2"
        ));

        assertNotNull(result);
        assertTrue(result.contains("[agent1]"));
        assertTrue(result.contains("[agent2]"));
    }

    @Test
    void testParallelExecuteWithMissingAgent() {
        bus.register("agent1", createMockAgent("agent1"));

        String result = bus.parallelExecute(Map.of(
                "agent1", "任务1",
                "missing", "任务2"
        ));

        assertTrue(result.contains("Agent not found: missing"));
    }

    private Agent createMockAgent(String name) {
        ChatProviderRegistry registry = new ChatProviderRegistry();
        registry.registerProvider(new MockProvider());
        registry.setDefaultProvider("mock");

        AiChat chat = new com.thinkai4j.core.api.DefaultAiChat(registry);
        return new Agent(name, "你是助手", chat);
    }

    private static class MockProvider implements ChatProvider {
        @Override
        public String getProviderName() { return "mock"; }

        @Override
        public AiResponse chat(ChatRequest request) {
            return new AiResponse("Mock: " +
                    request.getMessages().get(request.getMessages().size() - 1).getContent());
        }

        @Override
        public reactor.core.publisher.Flux<String> stream(ChatRequest request) {
            return reactor.core.publisher.Flux.just("mock");
        }
    }
}

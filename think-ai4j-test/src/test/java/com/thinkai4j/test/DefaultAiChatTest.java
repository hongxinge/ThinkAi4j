package com.thinkai4j.test;

import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.api.DefaultAiChat;
import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.memory.InMemoryChatMemory;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAiChatTest {

    private ChatProviderRegistry registry;
    private DefaultAiChat chat;

    @BeforeEach
    void setUp() {
        registry = new ChatProviderRegistry();
        registry.registerProvider(new MockChatProvider());
        registry.setDefaultProvider("mock");

        chat = new DefaultAiChat(registry);
    }

    @Test
    void testAsk() {
        String result = chat.ask("你好");
        assertNotNull(result);
        assertTrue(result.contains("Mock response"));
    }

    @Test
    void testSystem() {
        chat.system("你是助手");
        String result = chat.ask("你好");
        assertNotNull(result);
    }

    @Test
    void testProvider() {
        chat.provider("mock");
        String result = chat.ask("你好");
        assertNotNull(result);
    }

    @Test
    void testProviderNotFound() {
        chat.provider("nonexistent");
        assertThrows(AiException.class, () -> chat.ask("你好"));
    }

    @Test
    void testTemperature() {
        chat.temperature(0.7);
        String result = chat.ask("你好");
        assertNotNull(result);
    }

    @Test
    void testMaxTokens() {
        chat.maxTokens(100);
        String result = chat.ask("你好");
        assertNotNull(result);
    }

    @Test
    void testMemory() {
        ChatMemory memory = new InMemoryChatMemory();
        DefaultAiChat chatWithMemory = new DefaultAiChat(registry, memory);
        
        chatWithMemory.memory("conv1").ask("问题1");
        chatWithMemory.memory("conv1").ask("问题2");
        
        List<AiMessage> messages = memory.getMessages("conv1");
        assertEquals(4, messages.size());
    }

    @Test
    void testChatWithDirectRequest() {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("直接请求")))
                .build();
        
        AiResponse response = chat.chat(request);
        assertNotNull(response);
        assertTrue(response.getContent().contains("Mock response"));
    }

    @Test
    void testStream() {
        Flux<String> flux = chat.stream("流式请求");
        StepVerifier.create(flux)
                .expectNext("mock")
                .expectNext("stream")
                .verifyComplete();
    }

    @Test
    void testToolRegistrationAndExecution() {
        ToolDefinition toolDef = new ToolDefinition();
        toolDef.setFunction(new ToolDefinition.FunctionDefinition());
        toolDef.getFunction().setName("greet");
        toolDef.getFunction().setDescription("问候工具");
        toolDef.getFunction().addParameter("name", "string", "名字", true);

        chat.registerTool(toolDef, args -> "Hello, " + args);

        AiResponse response = chat.chatWithTools("你好");
        assertNotNull(response);
    }

    @Test
    void testToolDefinitions() {
        ToolDefinition toolDef = new ToolDefinition();
        toolDef.setFunction(new ToolDefinition.FunctionDefinition());
        toolDef.getFunction().setName("test_tool");
        toolDef.getFunction().setDescription("测试");

        chat.toolDefinitions(List.of(toolDef));
    }

    @Test
    void testChainedMethods() {
        String result = chat.system("你是助手")
                .provider("mock")
                .temperature(0.8)
                .maxTokens(200)
                .ask("链式调用");
        assertNotNull(result);
    }

    private static class MockChatProvider implements ChatProvider {
        private static int callCount = 0;

        @Override
        public String getProviderName() {
            return "mock";
        }

        @Override
        public AiResponse chat(ChatRequest request) {
            callCount++;
            String lastMessage = request.getMessages().get(request.getMessages().size() - 1).getContent();
            
            if (request.getTools() != null && !request.getTools().isEmpty() && callCount % 2 == 1) {
                ToolCall toolCall = new ToolCall();
                toolCall.setId("call_123");
                toolCall.setType("function");
                ToolCall.FunctionCall funcCall = new ToolCall.FunctionCall();
                funcCall.setName(request.getTools().get(0).getFunction().getName());
                funcCall.setArguments("{}");
                toolCall.setFunction(funcCall);
                
                AiResponse response = new AiResponse();
                response.setToolCalls(List.of(toolCall));
                return response;
            }
            return new AiResponse("Mock response for: " + lastMessage);
        }

        @Override
        public Flux<String> stream(ChatRequest request) {
            return Flux.just("mock", "stream");
        }
    }
}

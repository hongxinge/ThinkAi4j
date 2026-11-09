package com.thinkai4j.test;

import com.thinkai4j.agent.Agent;
import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.api.DefaultAiChat;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.MessageType;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.rag.Document;
import com.thinkai4j.rag.InMemoryDocumentStore;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P0 修复的回归用例：工具结果回传、Agent 工具循环、RAG 增量索引重建。
 * 三个用例在修复前的实现上均会失败。
 */
class P0FixRegressionTest {

    /** 记录每轮请求的消息列表；首轮可返回 tool_calls 且 content 为 null。 */
    private static class RecordingProvider implements ChatProvider {

        private final List<List<AiMessage>> requests = new ArrayList<>();
        private final AtomicInteger callCount = new AtomicInteger();
        private final boolean toolFirst;

        RecordingProvider(boolean toolFirst) {
            this.toolFirst = toolFirst;
        }

        @Override
        public String getProviderName() {
            return "mock";
        }

        @Override
        public Flux<String> stream(ChatRequest request) {
            return Flux.empty();
        }

        @Override
        public AiResponse chat(ChatRequest request) {
            requests.add(new ArrayList<>(request.getMessages()));
            AiResponse response = new AiResponse();
            if (toolFirst && callCount.getAndIncrement() == 0) {
                ToolCall.FunctionCall fn = new ToolCall.FunctionCall();
                fn.setName("echo");
                fn.setArguments("{}");
                ToolCall call = new ToolCall();
                call.setId("call-1");
                call.setType("function");
                call.setFunction(fn);
                response.setToolCalls(List.of(call));
                response.setContent(null);
            } else {
                response.setContent("最终答案");
            }
            return response;
        }
    }

    private ChatProviderRegistry registryWith(RecordingProvider provider) {
        ChatProviderRegistry registry = new ChatProviderRegistry();
        registry.registerProvider(provider);
        registry.setDefaultProvider("mock");
        return registry;
    }

    @Test
    void toolResultIsSentBackInNextRound() {
        RecordingProvider provider = new RecordingProvider(true);
        DefaultAiChat chat = new DefaultAiChat(registryWith(provider));

        ToolDefinition.FunctionDefinition fn = new ToolDefinition.FunctionDefinition();
        fn.setName("echo");
        fn.setDescription("回显");
        ToolDefinition definition = new ToolDefinition();
        definition.setFunction(fn);
        chat.registerTool(definition, args -> "工具返回");

        assertEquals("最终答案", chat.ask("你好"));
        assertEquals(2, provider.requests.size());
        assertTrue(provider.requests.get(1).stream()
                        .anyMatch(m -> m.getRole() == MessageType.TOOL),
                "第二轮请求必须携带 tool 角色消息");
    }

    @Test
    void agentKeepsLoopingWhenToolCallContentIsNull() {
        RecordingProvider provider = new RecordingProvider(true);
        AiChat chat = new DefaultAiChat(registryWith(provider));
        Agent agent = new Agent("测试", "你是助手", chat);

        assertEquals("最终答案", agent.execute("你好"));
        assertEquals(2, provider.requests.size(), "工具轮与最终轮都应发起请求");
    }

    @Test
    void incrementalIngestWithSameVocabularyIsSearchable() {
        InMemoryDocumentStore store = new InMemoryDocumentStore();
        String text = "公司规定年假为十五天";
        store.addDocuments(List.of(new Document(text)));
        // 第二次灌入不引入新词，旧实现会跳过词表与向量重建
        store.addDocuments(List.of(new Document(text)));

        List<Document> hits = store.search("年假", 3);
        assertEquals(2, hits.size(), "增量灌入的文档也必须建立向量并参与检索");
    }
}

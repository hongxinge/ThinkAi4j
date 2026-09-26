package com.thinkai4j.core.api;

import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.MessageType;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class DefaultAiChat implements AiChat {

    private final ChatProviderRegistry providerRegistry;
    private final ChatMemory chatMemory;
    private final ConcurrentHashMap<String, Function<String, String>> toolFunctions;
    private final List<ToolDefinition> toolDefinitions;

    private String providerName;
    private String conversationId;
    private final List<AiMessage> messages = new CopyOnWriteArrayList<>();
    private Double temperature;
    private Integer maxTokens;

    public DefaultAiChat(ChatProviderRegistry providerRegistry) {
        this(providerRegistry, null);
    }

    public DefaultAiChat(ChatProviderRegistry providerRegistry, ChatMemory chatMemory) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry cannot be null");
        this.chatMemory = chatMemory;
        this.toolFunctions = new ConcurrentHashMap<>();
        this.toolDefinitions = new CopyOnWriteArrayList<>();
    }

    @Override
    public String ask(String question) {
        if (question == null || question.isBlank()) {
            throw new AiException("question cannot be null or blank");
        }
        RequestContext ctx = buildRequestContext(question);
        return askInternal(ctx);
    }

    @Override
    public DefaultAiChat system(String content) {
        if (content != null && !content.isBlank()) {
            // 替换语义：移除已有 system 消息，避免链式多次调用导致堆叠
            messages.removeIf(m -> m.getRole() == MessageType.SYSTEM);
            messages.add(0, AiMessage.system(content));
        }
        return this;
    }

    @Override
    public DefaultAiChat provider(String providerName) {
        this.providerName = providerName;
        return this;
    }

    @Override
    public DefaultAiChat temperature(double temperature) {
        this.temperature = temperature;
        return this;
    }

    @Override
    public DefaultAiChat maxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    @Override
    public DefaultAiChat memory(String conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public DefaultAiChat registerTool(ToolDefinition definition, Function<String, String> executor) {
        if (definition != null && definition.getFunction() != null && executor != null) {
            this.toolDefinitions.add(definition);
            this.toolFunctions.put(definition.getFunction().getName(), executor);
        }
        return this;
    }

    public DefaultAiChat toolDefinitions(List<ToolDefinition> definitions) {
        if (definitions != null) {
            this.toolDefinitions.addAll(definitions);
        }
        return this;
    }

    @Override
    public Flux<String> stream(String question) {
        if (question == null || question.isBlank()) {
            throw new AiException("question cannot be null or blank");
        }
        RequestContext ctx = buildRequestContext(question);
        ChatRequest request = buildRequest(ctx, toolDefinitions, true);
        // 与 ask() 保持一致：流式对话同样写入记忆
        saveToMemory(ctx.conversationId, AiMessage.user(ctx.question));
        StringBuilder contentBuffer = new StringBuilder();
        return getProvider(ctx.providerName).stream(request)
                .doOnNext(contentBuffer::append)
                .doOnComplete(() -> saveToMemory(ctx.conversationId, AiMessage.assistant(contentBuffer.toString())));
    }

    @Override
    public AiResponse chatWithTools(String question) {
        return chatWithTools(question, null);
    }

    @Override
    public AiResponse chat(ChatRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        // provider 字段用于选择 Provider（为空时使用默认 Provider）；
        // model 字段保持用户设置不变，为空时由 Provider 使用配置的默认模型
        return getProvider(request.getProvider()).chat(request);
    }
    private String askInternal(RequestContext ctx) {
        int maxIterations = 5;
        List<AiMessage> history = new ArrayList<>(ctx.history);

        for (int i = 0; i < maxIterations; i++) {
            ChatRequest request = buildRequest(ctx, toolDefinitions, false);
            AiResponse response = getProvider(ctx.providerName).chat(request);

            if (hasToolCalls(response) && !toolFunctions.isEmpty()) {
                history.add(createAssistantMessageWithToolCalls(response.getToolCalls()));
                executeTools(response, history);
                continue;
            }

            if (response.getContent() != null && !response.getContent().isEmpty()) {
                saveToMemory(ctx.conversationId, AiMessage.user(ctx.question));
                saveToMemory(ctx.conversationId, AiMessage.assistant(response.getContent()));
                return response.getContent();
            }
        }

        throw new AiException("Max tool iterations reached without final answer");
    }

    private AiResponse chatWithTools(String question, String conversationId) {
        if (question == null || question.isBlank()) {
            throw new AiException("question cannot be null or blank");
        }
        RequestContext ctx = buildRequestContext(question, conversationId);
        int maxIterations = 5;
        List<AiMessage> history = new ArrayList<>(ctx.history);

        for (int i = 0; i < maxIterations; i++) {
            ChatRequest request = buildRequest(ctx, toolDefinitions, false);
            AiResponse response = getProvider(ctx.providerName).chat(request);

            if (hasToolCalls(response) && !toolFunctions.isEmpty()) {
                history.add(createAssistantMessageWithToolCalls(response.getToolCalls()));
                executeTools(response, history);
                continue;
            }

            if (response.getContent() != null && !response.getContent().isEmpty()) {
                saveToMemory(ctx.conversationId, AiMessage.user(question));
                saveToMemory(ctx.conversationId, AiMessage.assistant(response.getContent()));
            }
            return response;
        }

        throw new AiException("Max tool iterations reached without final answer");
    }

    private RequestContext buildRequestContext(String question) {
        return buildRequestContext(question, this.conversationId);
    }

    private RequestContext buildRequestContext(String question, String conversationId) {
        RequestContext ctx = new RequestContext();
        ctx.question = question;
        ctx.conversationId = conversationId != null ? conversationId : this.conversationId;
        ctx.providerName = this.providerName;
        ctx.history = resolveHistory(ctx.conversationId, question);
        return ctx;
    }

    private List<AiMessage> resolveHistory(String conversationId, String question) {
        List<AiMessage> history = new ArrayList<>();
        if (conversationId != null && chatMemory != null) {
            List<AiMessage> stored = chatMemory.getMessages(conversationId);
            if (stored != null) {
                history.addAll(stored);
            }
        }
        history.addAll(new ArrayList<>(messages));
        history.add(AiMessage.user(question));
        return history;
    }

    private ChatRequest buildRequest(RequestContext ctx, List<ToolDefinition> tools, boolean stream) {
        ChatRequest.Builder builder = ChatRequest.builder().messages(new ArrayList<>(ctx.history));
        if (temperature != null) builder.temperature(temperature);
        if (maxTokens != null) builder.maxTokens(maxTokens);
        if (tools != null && !tools.isEmpty()) builder.tools(tools);
        ChatRequest request = builder.build();
        request.setStream(stream);
        // 不再写入 model：保持为 null，由 Provider 使用配置的默认模型，
        // 避免把 provider 名（如 doubao）误当作模型名发送给 API
        return request;
    }

    private String getProviderName(String providerName) {
        String name = providerName != null ? providerName : providerRegistry.getDefaultProvider();
        if (name == null || name.isEmpty()) {
            throw new AiException("No provider specified and no default provider configured");
        }
        return name;
    }

    private ChatProvider getProvider(String providerName) {
        String name = getProviderName(providerName);
        ChatProvider provider = providerRegistry.getProvider(name);
        if (provider == null) {
            throw new AiException("Provider not found: " + name + ", available: " + providerRegistry.getAvailableProviders());
        }
        return provider;
    }

    private boolean hasToolCalls(AiResponse response) {
        return response.getToolCalls() != null && !response.getToolCalls().isEmpty();
    }

    private void executeTools(AiResponse response, List<AiMessage> history) {
        for (ToolCall toolCall : response.getToolCalls()) {
            String toolName = toolCall.getFunction().getName();
            String toolArgs = toolCall.getFunction().getArguments();
            Function<String, String> executor = toolFunctions.get(toolName);
            String result = (executor != null) ? executor.apply(toolArgs) : "";
            history.add(createToolResultMessage(toolCall, result));
        }
    }

    private void saveToMemory(String conversationId, AiMessage message) {
        if (conversationId != null && chatMemory != null) {
            chatMemory.addMessage(conversationId, message);
        }
    }

    private AiMessage createAssistantMessageWithToolCalls(List<ToolCall> toolCalls) {
        AiMessage message = new AiMessage(com.thinkai4j.core.model.MessageType.ASSISTANT, null);
        message.setToolCalls(toolCalls);
        return message;
    }

    private AiMessage createToolResultMessage(ToolCall toolCall, String result) {
        AiMessage message = new AiMessage(com.thinkai4j.core.model.MessageType.TOOL, result);
        message.setToolCallId(toolCall.getId());
        message.setName(toolCall.getFunction().getName());
        return message;
    }

    private static class RequestContext {
        String question;
        String conversationId;
        String providerName;
        List<AiMessage> history;
    }
}

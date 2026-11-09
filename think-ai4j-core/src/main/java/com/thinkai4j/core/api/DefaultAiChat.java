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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * AiChat 的默认实现。
 *
 * <p>链式配置方法返回携带新上下文快照的实例，不再修改自身状态；因此该对象可作为
 * Spring 单例安全共享，并发请求之间不会互相覆盖 provider/system/memory 配置。
 * 工具注册表在同一应用内共享，链式派生出的实例沿用同一引用。
 */
public class DefaultAiChat implements AiChat {

    private static final int MAX_TOOL_ITERATIONS = 5;

    private final ChatProviderRegistry providerRegistry;
    private final ChatMemory chatMemory;
    private final ConcurrentHashMap<String, Function<String, String>> toolFunctions;
    private final List<ToolDefinition> toolDefinitions;
    private final ChatOptions options;

    public DefaultAiChat(ChatProviderRegistry providerRegistry) {
        this(providerRegistry, null);
    }

    public DefaultAiChat(ChatProviderRegistry providerRegistry, ChatMemory chatMemory) {
        this(providerRegistry, chatMemory, new ConcurrentHashMap<>(),
                new CopyOnWriteArrayList<>(), ChatOptions.empty());
    }

    private DefaultAiChat(ChatProviderRegistry providerRegistry, ChatMemory chatMemory,
                          ConcurrentHashMap<String, Function<String, String>> toolFunctions,
                          List<ToolDefinition> toolDefinitions, ChatOptions options) {
        this.providerRegistry = Objects.requireNonNull(providerRegistry, "providerRegistry cannot be null");
        this.chatMemory = chatMemory;
        this.toolFunctions = toolFunctions;
        this.toolDefinitions = toolDefinitions;
        this.options = options;
    }

    private DefaultAiChat derive(ChatOptions newOptions) {
        return new DefaultAiChat(providerRegistry, chatMemory, toolFunctions, toolDefinitions, newOptions);
    }

    @Override
    public String ask(String question) {
        if (question == null || question.isBlank()) {
            throw new AiException("question cannot be null or blank");
        }
        return askInternal(buildRequestContext(question));
    }

    @Override
    public DefaultAiChat system(String content) {
        if (content == null || content.isBlank()) {
            return this;
        }
        // 替换语义：移除已有系统提示词，仅保留最新一条
        return derive(options.withSystem(content));
    }

    @Override
    public DefaultAiChat provider(String providerName) {
        return derive(options.withProvider(providerName));
    }

    @Override
    public DefaultAiChat temperature(double temperature) {
        return derive(options.withTemperature(temperature));
    }

    @Override
    public DefaultAiChat maxTokens(int maxTokens) {
        return derive(options.withMaxTokens(maxTokens));
    }

    @Override
    public DefaultAiChat memory(String conversationId) {
        return derive(options.withConversationId(conversationId));
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
        ChatRequest request = buildRequest(ctx.history, true);
        // 每次订阅独立缓冲区；用户消息在订阅时才写入，避免装配期写入造成记忆不完整
        return Flux.defer(() -> {
            saveToMemory(ctx.conversationId, AiMessage.user(ctx.question));
            StringBuilder contentBuffer = new StringBuilder();
            return getProvider(ctx.providerName).stream(request)
                    .doOnNext(contentBuffer::append)
                    .doOnComplete(() -> saveToMemory(ctx.conversationId,
                            AiMessage.assistant(contentBuffer.toString())));
        });
    }

    @Override
    public AiResponse chatWithTools(String question) {
        return chatWithTools(question, null);
    }

    @Override
    public AiResponse chat(ChatRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        // provider 字段用于选择 Provider，model 字段保持用户设置不变
        return getProvider(request.getProvider()).chat(request);
    }

    private String askInternal(RequestContext ctx) {
        List<AiMessage> history = new ArrayList<>(ctx.history);
        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            AiResponse response = getProvider(ctx.providerName).chat(buildRequest(history, false));
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
        List<AiMessage> history = new ArrayList<>(ctx.history);
        for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
            AiResponse response = getProvider(ctx.providerName).chat(buildRequest(history, false));
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
        return buildRequestContext(question, options.conversationId);
    }

    private RequestContext buildRequestContext(String question, String conversationId) {
        RequestContext ctx = new RequestContext();
        ctx.question = question;
        ctx.conversationId = conversationId != null ? conversationId : options.conversationId;
        ctx.providerName = options.providerName;
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
        history.addAll(options.messages);
        history.add(AiMessage.user(question));
        return history;
    }

    private ChatRequest buildRequest(List<AiMessage> history, boolean stream) {
        ChatRequest.Builder builder = ChatRequest.builder().messages(new ArrayList<>(history));
        if (options.temperature != null) {
            builder.temperature(options.temperature);
        }
        if (options.maxTokens != null) {
            builder.maxTokens(options.maxTokens);
        }
        if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
            builder.tools(toolDefinitions);
        }
        ChatRequest request = builder.build();
        request.setStream(stream);
        // 不写入 model：保持为 null，由 Provider 使用配置的默认模型
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
            throw new AiException("Provider not found: " + name
                    + ", available: " + providerRegistry.getAvailableProviders());
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
        AiMessage message = new AiMessage(MessageType.ASSISTANT, null);
        message.setToolCalls(toolCalls);
        return message;
    }

    private AiMessage createToolResultMessage(ToolCall toolCall, String result) {
        AiMessage message = new AiMessage(MessageType.TOOL, result);
        message.setToolCallId(toolCall.getId());
        message.setName(toolCall.getFunction().getName());
        return message;
    }

    /** 不可变的链式配置快照，每次 fluent 调用生成新实例。 */
    private static final class ChatOptions {

        private final String providerName;
        private final String conversationId;
        private final List<AiMessage> messages;
        private final Double temperature;
        private final Integer maxTokens;

        private ChatOptions(String providerName, String conversationId, List<AiMessage> messages,
                            Double temperature, Integer maxTokens) {
            this.providerName = providerName;
            this.conversationId = conversationId;
            this.messages = List.copyOf(messages);
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }

        static ChatOptions empty() {
            return new ChatOptions(null, null, List.of(), null, null);
        }

        ChatOptions withSystem(String content) {
            List<AiMessage> next = new ArrayList<>();
            for (AiMessage message : messages) {
                if (message.getRole() != MessageType.SYSTEM) {
                    next.add(message);
                }
            }
            next.add(0, AiMessage.system(content));
            return new ChatOptions(providerName, conversationId, next, temperature, maxTokens);
        }

        ChatOptions withProvider(String name) {
            return new ChatOptions(name, conversationId, messages, temperature, maxTokens);
        }

        ChatOptions withConversationId(String id) {
            return new ChatOptions(providerName, id, messages, temperature, maxTokens);
        }

        ChatOptions withTemperature(double value) {
            return new ChatOptions(providerName, conversationId, messages, value, maxTokens);
        }

        ChatOptions withMaxTokens(int value) {
            return new ChatOptions(providerName, conversationId, messages, temperature, value);
        }
    }

    private static class RequestContext {
        String question;
        String conversationId;
        String providerName;
        List<AiMessage> history;
    }
}

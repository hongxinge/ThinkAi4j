package com.thinkai4j.core.api;

import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.memory.ChatMemory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DefaultAiChat implements AiChat {

    private final ChatProviderRegistry providerRegistry;
    private final ChatMemory chatMemory;
    private final Map<String, Function<String, String>> toolFunctions;

    private String providerName;
    private String conversationId;
    private final List<AiMessage> messages = new ArrayList<>();
    private Double temperature;
    private Integer maxTokens;
    private List<ToolDefinition> toolDefinitions;

    public DefaultAiChat(ChatProviderRegistry providerRegistry) {
        this(providerRegistry, null);
    }

    public DefaultAiChat(ChatProviderRegistry providerRegistry, ChatMemory chatMemory) {
        this.providerRegistry = providerRegistry;
        this.chatMemory = chatMemory;
        this.toolFunctions = new java.util.HashMap<>();
    }

    @Override
    public String ask(String question) {
        return askInternal(question, null);
    }

    @Override
    public DefaultAiChat system(String content) {
        messages.add(0, AiMessage.system(content));
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

    @Override
    public Flux<String> stream(String question) {
        List<AiMessage> history = resolveHistory();
        history.add(AiMessage.user(question));

        ChatRequest request = buildRequest(history);
        request.setStream(true);

        return getProvider().stream(request);
    }

    @Override
    public AiResponse chatWithTools(String question) {
        return chatWithTools(question, null);
    }

    public DefaultAiChat registerTool(ToolDefinition definition, Function<String, String> executor) {
        if (this.toolDefinitions == null) {
            this.toolDefinitions = new ArrayList<>();
        }
        this.toolDefinitions.add(definition);
        this.toolFunctions.put(definition.getFunction().getName(), executor);
        return this;
    }

    public DefaultAiChat toolDefinitions(List<ToolDefinition> definitions) {
        this.toolDefinitions = definitions;
        return this;
    }

    private String askInternal(String question, String conversationId) {
        String cid = conversationId != null ? conversationId : this.conversationId;
        List<AiMessage> history = resolveHistoryForChat(cid, question);

        int maxIterations = 5;
        for (int i = 0; i < maxIterations; i++) {
            ChatRequest request = buildRequest(history);

            if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
                request.setTools(toolDefinitions);
            }

            AiResponse response = getProvider().chat(request);

            if (response.getToolCalls() != null && !response.getToolCalls().isEmpty() && !toolFunctions.isEmpty()) {
                history.add(createAssistantMessageWithToolCalls(response.getToolCalls()));

                for (ToolCall toolCall : response.getToolCalls()) {
                    String toolName = toolCall.getFunction().getName();
                    String toolArgs = toolCall.getFunction().getArguments();
                    Function<String, String> executor = toolFunctions.get(toolName);

                    String result = "";
                    if (executor != null) {
                        result = executor.apply(toolArgs);
                    }
                    history.add(createToolResultMessage(toolCall, result));
                }
                continue;
            }

            if (response.getContent() != null && !response.getContent().isEmpty()) {
                saveToMemory(cid, AiMessage.user(question));
                saveToMemory(cid, AiMessage.assistant(response.getContent()));
                clearMessages();
                return response.getContent();
            }

            clearMessages();
            return response.getContent();
        }

        clearMessages();
        return null;
    }

    private AiResponse chatWithTools(String question, String conversationId) {
        String cid = conversationId != null ? conversationId : this.conversationId;
        List<AiMessage> history = resolveHistoryForChat(cid, question);

        int maxIterations = 5;
        for (int i = 0; i < maxIterations; i++) {
            ChatRequest request = buildRequest(history);

            if (toolDefinitions != null && !toolDefinitions.isEmpty()) {
                request.setTools(toolDefinitions);
            }

            AiResponse response = getProvider().chat(request);

            if (response.getToolCalls() != null && !response.getToolCalls().isEmpty() && !toolFunctions.isEmpty()) {
                history.add(createAssistantMessageWithToolCalls(response.getToolCalls()));

                for (ToolCall toolCall : response.getToolCalls()) {
                    String toolName = toolCall.getFunction().getName();
                    String toolArgs = toolCall.getFunction().getArguments();
                    Function<String, String> executor = toolFunctions.get(toolName);

                    String result = "";
                    if (executor != null) {
                        result = executor.apply(toolArgs);
                    }
                    history.add(createToolResultMessage(toolCall, result));
                }
                continue;
            }

            if (response.getContent() != null && !response.getContent().isEmpty()) {
                saveToMemory(cid, AiMessage.user(question));
                saveToMemory(cid, AiMessage.assistant(response.getContent()));
            }

            clearMessages();
            return response;
        }

        clearMessages();
        return null;
    }

    private List<AiMessage> resolveHistoryForChat(String conversationId, String question) {
        if (conversationId != null && chatMemory != null) {
            List<AiMessage> history = new ArrayList<>(chatMemory.getMessages(conversationId));
            history.add(AiMessage.user(question));
            return history;
        }

        List<AiMessage> history = new ArrayList<>();
        history.addAll(messages);
        history.add(AiMessage.user(question));
        return history;
    }

    private List<AiMessage> resolveHistory() {
        if (conversationId != null && chatMemory != null) {
            return new ArrayList<>(chatMemory.getMessages(conversationId));
        }
        return new ArrayList<>(messages);
    }

    private ChatRequest buildRequest(List<AiMessage> history) {
        ChatRequest.Builder builder = ChatRequest.builder()
                .messages(new ArrayList<>(history));

        if (temperature != null) {
            builder.temperature(temperature);
        }
        if (maxTokens != null) {
            builder.maxTokens(maxTokens);
        }

        ChatRequest request = builder.build();
        request.setModel(getProviderName());
        return request;
    }

    private ChatProvider getProvider() {
        String name = getProviderName();
        ChatProvider provider = providerRegistry.getProvider(name);
        if (provider == null) {
            throw new AiException("Provider not found: " + name + ", available providers: " + providerRegistry.getAvailableProviders());
        }
        return provider;
    }

    private String getProviderName() {
        return providerName != null ? providerName : providerRegistry.getDefaultProvider();
    }

    private void saveToMemory(String conversationId, AiMessage message) {
        if (conversationId != null && chatMemory != null) {
            chatMemory.addMessage(conversationId, message);
        }
    }

    private void clearMessages() {
        messages.clear();
        this.conversationId = null;
        this.providerName = null;
        this.temperature = null;
        this.maxTokens = null;
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
}

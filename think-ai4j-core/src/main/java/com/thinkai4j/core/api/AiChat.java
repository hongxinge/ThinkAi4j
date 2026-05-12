package com.thinkai4j.core.api;

import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolDefinition;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiChat {

    String ask(String question);

    AiChat system(String content);

    AiChat provider(String providerName);

    AiChat temperature(double temperature);

    AiChat maxTokens(int maxTokens);

    AiChat memory(String conversationId);

    Flux<String> stream(String question);

    AiResponse chatWithTools(String question);

    AiResponse chat(ChatRequest request);

    AiChat registerTool(ToolDefinition definition, java.util.function.Function<String, String> executor);

    AiChat toolDefinitions(List<ToolDefinition> definitions);
}

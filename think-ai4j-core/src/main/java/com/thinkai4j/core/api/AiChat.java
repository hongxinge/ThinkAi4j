package com.thinkai4j.core.api;

import com.thinkai4j.core.model.AiResponse;
import reactor.core.publisher.Flux;

public interface AiChat {

    String ask(String question);

    AiChat system(String content);

    AiChat provider(String providerName);

    AiChat temperature(double temperature);

    AiChat maxTokens(int maxTokens);

    AiChat memory(String conversationId);

    Flux<String> stream(String question);

    AiResponse chatWithTools(String question);
}

package com.thinkai4j.core.api;

import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.AiResponse;
import reactor.core.publisher.Flux;

public interface ChatProvider {

    String getProviderName();

    AiResponse chat(ChatRequest request);

    Flux<String> stream(ChatRequest request);
}

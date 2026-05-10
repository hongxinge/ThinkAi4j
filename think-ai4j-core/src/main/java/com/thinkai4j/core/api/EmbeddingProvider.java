package com.thinkai4j.core.api;

import com.thinkai4j.core.model.EmbeddingRequest;
import com.thinkai4j.core.model.EmbeddingResponse;

public interface EmbeddingProvider {

    String getProviderName();

    EmbeddingResponse embed(EmbeddingRequest request);
}

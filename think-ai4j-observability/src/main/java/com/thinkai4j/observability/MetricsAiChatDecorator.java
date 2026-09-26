package com.thinkai4j.observability;

import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolDefinition;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * AiChat 指标采集装饰器。
 *
 * <p>包装任意 {@link AiChat} 实现，在 ask / chat / chatWithTools / stream
 * 调用前后自动记录请求总数、错误数、耗时与 Token 消耗，
 * 由 Spring Boot Starter 在 classpath 存在 Micrometer 时自动装配。</p>
 */
public class MetricsAiChatDecorator implements AiChat {

    private static final String UNKNOWN_PROVIDER = "unknown";

    private final AiChat delegate;
    private final AiMetricsCollector metrics;

    public MetricsAiChatDecorator(AiChat delegate, AiMetricsCollector metrics) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics cannot be null");
    }

    @Override
    public String ask(String question) {
        metrics.recordRequestStart();
        long start = System.nanoTime();
        try {
            String answer = delegate.ask(question);
            metrics.recordRequestSuccess(Duration.ofNanos(System.nanoTime() - start), null, UNKNOWN_PROVIDER);
            return answer;
        } catch (RuntimeException e) {
            metrics.recordRequestError();
            throw e;
        }
    }

    @Override
    public AiResponse chat(ChatRequest request) {
        metrics.recordRequestStart();
        long start = System.nanoTime();
        try {
            AiResponse response = delegate.chat(request);
            String provider = request.getProvider() != null ? request.getProvider() : UNKNOWN_PROVIDER;
            metrics.recordRequestSuccess(Duration.ofNanos(System.nanoTime() - start),
                    response != null ? response.getUsage() : null, provider);
            return response;
        } catch (RuntimeException e) {
            metrics.recordRequestError();
            throw e;
        }
    }

    @Override
    public AiResponse chatWithTools(String question) {
        metrics.recordRequestStart();
        long start = System.nanoTime();
        try {
            AiResponse response = delegate.chatWithTools(question);
            metrics.recordRequestSuccess(Duration.ofNanos(System.nanoTime() - start),
                    response != null ? response.getUsage() : null, UNKNOWN_PROVIDER);
            return response;
        } catch (RuntimeException e) {
            metrics.recordRequestError();
            throw e;
        }
    }

    @Override
    public Flux<String> stream(String question) {
        metrics.recordRequestStart();
        long start = System.nanoTime();
        return delegate.stream(question)
                .doOnComplete(() -> metrics.recordRequestSuccess(
                        Duration.ofNanos(System.nanoTime() - start), null, UNKNOWN_PROVIDER))
                .doOnError(e -> metrics.recordRequestError());
    }

    @Override
    public AiChat system(String content) {
        delegate.system(content);
        return this;
    }

    @Override
    public AiChat provider(String providerName) {
        delegate.provider(providerName);
        return this;
    }

    @Override
    public AiChat temperature(double temperature) {
        delegate.temperature(temperature);
        return this;
    }

    @Override
    public AiChat maxTokens(int maxTokens) {
        delegate.maxTokens(maxTokens);
        return this;
    }

    @Override
    public AiChat memory(String conversationId) {
        delegate.memory(conversationId);
        return this;
    }

    @Override
    public AiChat registerTool(ToolDefinition definition, Function<String, String> executor) {
        delegate.registerTool(definition, executor);
        return this;
    }

    @Override
    public AiChat toolDefinitions(List<ToolDefinition> definitions) {
        delegate.toolDefinitions(definitions);
        return this;
    }

    /**
     * 获取被包装的原始 AiChat 实例。
     */
    public AiChat getDelegate() {
        return delegate;
    }
}

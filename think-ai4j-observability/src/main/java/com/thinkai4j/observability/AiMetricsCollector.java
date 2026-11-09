package com.thinkai4j.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.thinkai4j.core.model.Usage;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class AiMetricsCollector {

    private final MeterRegistry registry;
    private final Counter totalRequestsCounter;
    private final Counter errorCounter;
    private final Counter totalTokensCounter;
    private final Timer requestDurationTimer;
    private final AtomicLong activeRequests = new AtomicLong(0);

    public AiMetricsCollector(MeterRegistry registry) {
        this.registry = registry;
        this.totalRequestsCounter = Counter.builder("think.ai.requests.total")
                .description("Total AI API requests")
                .register(registry);

        this.errorCounter = Counter.builder("think.ai.errors.total")
                .description("Total AI API errors")
                .register(registry);

        // 与 recordRequestSuccess 中动态注册的同名 meter 保持一致的 tag key 集合
        // （type + provider），否则 Prometheus 后端会因标签不一致报错
        this.totalTokensCounter = Counter.builder("think.ai.tokens.total")
                .description("Total tokens consumed")
                .tag("type", "all")
                .tag("provider", "unknown")
                .register(registry);

        this.requestDurationTimer = Timer.builder("think.ai.request.duration")
                .description("AI API request duration")
                .register(registry);

        // 将进行中请求数注册为 Gauge，使 /actuator/metrics 可暴露该指标
        Gauge.builder("think.ai.requests.active", activeRequests, AtomicLong::get)
                .description("当前进行中的 AI API 请求数")
                .register(registry);
    }

    public void recordRequestStart() {
        totalRequestsCounter.increment();
        activeRequests.incrementAndGet();
    }

    public void recordRequestSuccess(Duration duration, Usage usage, String provider) {
        activeRequests.decrementAndGet();
        requestDurationTimer.record(duration);

        if (usage != null) {
            Counter.builder("think.ai.tokens.total")
                    .tag("type", "prompt")
                    .tag("provider", provider)
                    .register(registry)
                    .increment(usage.getPromptTokens());

            Counter.builder("think.ai.tokens.total")
                    .tag("type", "completion")
                    .tag("provider", provider)
                    .register(registry)
                    .increment(usage.getCompletionTokens());

            totalTokensCounter.increment(usage.getTotalTokens());
        }
    }

    /**
     * 记录一次失败请求（不记录耗时），保留原有调用方的兼容入口。
     */
    public void recordRequestError() {
        recordRequestError(null);
    }

    /**
     * 记录一次失败请求并记录其耗时；duration 为 null 时等价于旧的无参重载。
     */
    public void recordRequestError(Duration duration) {
        activeRequests.decrementAndGet();
        errorCounter.increment();
        if (duration != null) {
            requestDurationTimer.record(duration);
        }
    }

    public long getActiveRequests() {
        return activeRequests.get();
    }
}

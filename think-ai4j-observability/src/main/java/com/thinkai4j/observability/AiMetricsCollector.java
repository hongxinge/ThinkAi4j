package com.thinkai4j.observability;

import io.micrometer.core.instrument.Counter;
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

        this.totalTokensCounter = Counter.builder("think.ai.tokens.total")
                .description("Total tokens consumed")
                .tag("type", "all")
                .register(registry);

        this.requestDurationTimer = Timer.builder("think.ai.request.duration")
                .description("AI API request duration")
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

    public void recordRequestError() {
        activeRequests.decrementAndGet();
        errorCounter.increment();
    }

    public long getActiveRequests() {
        return activeRequests.get();
    }
}

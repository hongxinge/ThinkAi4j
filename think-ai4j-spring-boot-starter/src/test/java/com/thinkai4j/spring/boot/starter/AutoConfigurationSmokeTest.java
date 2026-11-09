package com.thinkai4j.spring.boot.starter;

import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.api.ChatProviderRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 自动装配冒烟测试：覆盖「仅引入 starter」的最小使用场景。
 *
 * <p>回归背景：1.0.2 中能力模块被标记为 optional，配置属性类 ThinkAiProperties
 * 的字段类型 OpenAiCompatConfig 无法加载，容器在启动阶段抛 NoClassDefFoundError。
 */
class AutoConfigurationSmokeTest {

    @Test
    void providerConfigRegistersDefaultProvider() {
        try (AnnotationConfigApplicationContext ctx = context(providerProperties())) {
            assertNotNull(ctx.getBean(AiChat.class));
            ChatProviderRegistry registry = ctx.getBean(ChatProviderRegistry.class);
            assertTrue(registry.getAvailableProviders().contains("doubao"));
            assertEquals("doubao", registry.getDefaultProvider());
        }
    }

    @Test
    void noProviderConfigStillStartsContext() {
        try (AnnotationConfigApplicationContext ctx = context(new HashMap<>())) {
            assertNotNull(ctx.getBean(AiChat.class));
            assertNotNull(ctx.getBean(ChatProviderRegistry.class));
        }
    }

    @Test
    void fluentConfigDoesNotMutateSharedInstance() {
        try (AnnotationConfigApplicationContext ctx = context(providerProperties())) {
            AiChat shared = ctx.getBean(AiChat.class);
            AiChat derived = shared.system("你是助手");
            assertNotSame(shared, derived, "链式配置应返回新实例，避免单例状态串扰");
        }
    }

    private Map<String, Object> providerProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("think.ai.compat.providers[0].name", "doubao");
        props.put("think.ai.compat.providers[0].baseUrl", "http://127.0.0.1:11434/v1");
        props.put("think.ai.compat.providers[0].apiKey", "test-key");
        props.put("think.ai.compat.providers[0].model", "test-model");
        return props;
    }

    private AnnotationConfigApplicationContext context(Map<String, Object> props) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("smoke-test", props));
        ctx.register(ThinkAiAutoConfiguration.class);
        ctx.refresh();
        return ctx;
    }
}

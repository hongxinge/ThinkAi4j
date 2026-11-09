package com.thinkai4j.spring.boot.starter;

import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.api.ChatProviderRegistry;
import com.thinkai4j.core.api.DefaultAiChat;
import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.memory.InMemoryChatMemory;
import com.thinkai4j.memory.redis.RedisChatMemory;
import com.thinkai4j.provider.compat.OpenAiCompatConfig;
import com.thinkai4j.provider.compat.OpenAiCompatProvider;
import com.thinkai4j.rag.DocumentStore;
import com.thinkai4j.rag.InMemoryDocumentStore;
import com.thinkai4j.rag.RagPipeline;
import com.thinkai4j.agent.Agent;
import com.thinkai4j.agent.AiAgent;
import com.thinkai4j.observability.AiMetricsCollector;
import com.thinkai4j.observability.MetricsAiChatDecorator;
import com.thinkai4j.tool.annotation.AiTool;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@EnableConfigurationProperties(ThinkAiProperties.class)
public class ThinkAiAutoConfiguration {

    /**
     * 根据 think.ai.compat.providers 配置构造并注册 OpenAI 兼容 Provider。
     *
     * <p>此前由独立配置类经 {@code List<ChatProvider>} Bean 注册，存在三处阻断：
     * 能力模块 optional 不传递导致配置属性类无法加载、OpenAiCompatConfig 无 Bean 注册点、
     * 集合注入与 @ConditionalOnProperty 对 YAML 列表的匹配不确定。
     * 现改为在注册中心创建时一次完成；未配置任何 Provider 时容器仍可正常启动。
     */
    @Bean
    @ConditionalOnMissingBean
    public ChatProviderRegistry chatProviderRegistry(ThinkAiProperties properties) {
        ChatProviderRegistry registry = new ChatProviderRegistry();
        registry.setDefaultProvider(properties.getDefaultProvider());

        OpenAiCompatConfig compat = properties.getCompat();
        if (compat == null || compat.getProviders() == null) {
            return registry;
        }
        for (OpenAiCompatConfig.ModelProvider config : compat.getProviders()) {
            ChatProvider provider = new OpenAiCompatProvider(config, compat.getHttpClient());
            registry.registerProvider(provider);
            if (registry.getDefaultProvider() == null) {
                registry.setDefaultProvider(provider.getProviderName());
            }
        }
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean(AiChat.class)
    public AiChat aiChat(ChatProviderRegistry registry, Optional<ChatMemory> chatMemory,
                         ObjectProvider<AiMetricsCollector> metricsCollectorProvider,
                         ThinkAiProperties properties) {
        boolean memoryEnabled = properties.getMemory() == null
                || !Boolean.FALSE.equals(properties.getMemory().getEnabled());
        ChatMemory memory = memoryEnabled ? chatMemory.orElse(null) : null;
        DefaultAiChat chat = new DefaultAiChat(registry, memory);
        AiMetricsCollector metrics = metricsCollectorProvider.getIfAvailable();
        // 存在指标采集器（通常由 actuator 的 MeterRegistry 触发）时包装调用链
        return metrics != null ? new MetricsAiChatDecorator(chat, metrics) : chat;
    }

    /**
     * 可观测性自动装配：引入 spring-boot-starter-actuator 后，
     * 自动创建指标采集器并把请求/错误/耗时/Token 指标挂进 AiChat 调用链。
     */
    @Configuration
    @ConditionalOnClass({AiMetricsCollector.class, MeterRegistry.class})
    @ConditionalOnBean(MeterRegistry.class)
    static class ObservabilityAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(AiMetricsCollector.class)
        public AiMetricsCollector aiMetricsCollector(MeterRegistry meterRegistry) {
            return new AiMetricsCollector(meterRegistry);
        }
    }

    @Configuration
    @ConditionalOnClass({ChatMemory.class, RedisChatMemory.class, StringRedisTemplate.class})
    @ConditionalOnProperty(prefix = "think.ai.memory", name = "type", havingValue = "redis")
    static class RedisMemoryAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatMemory.class)
        public ChatMemory redisChatMemory(StringRedisTemplate redisTemplate, ThinkAiProperties properties) {
            RedisChatMemory memory = new RedisChatMemory(redisTemplate);
            if (properties.getMemory() != null && properties.getMemory().getMaxMessages() != null) {
                memory.setMaxMessages(properties.getMemory().getMaxMessages());
            } else {
                memory.setMaxMessages(20);
            }
            if (properties.getMemory() != null && properties.getMemory().getTtlMinutes() != null) {
                memory.setTtlMinutes(properties.getMemory().getTtlMinutes());
            } else {
                memory.setTtlMinutes(60);
            }
            return memory;
        }
    }

    @Configuration
    @ConditionalOnClass(ChatMemory.class)
    @ConditionalOnProperty(prefix = "think.ai.memory", name = "type", havingValue = "memory", matchIfMissing = true)
    static class InMemoryAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(ChatMemory.class)
        public ChatMemory chatMemory(ThinkAiProperties properties) {
            InMemoryChatMemory memory = new InMemoryChatMemory();
            if (properties.getMemory() != null && properties.getMemory().getMaxMessages() != null) {
                memory.setMaxMessages(properties.getMemory().getMaxMessages());
            } else {
                memory.setMaxMessages(20);
            }
            return memory;
        }
    }

    @Configuration
    @ConditionalOnClass(RagPipeline.class)
    @ConditionalOnProperty(prefix = "think.ai.rag", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class RagAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(DocumentStore.class)
        public DocumentStore documentStore() {
            return new InMemoryDocumentStore();
        }

        @Bean
        @ConditionalOnMissingBean(RagPipeline.class)
        public RagPipeline ragPipeline(AiChat chat, DocumentStore documentStore,
                                       ThinkAiProperties properties) {
            Integer topK = properties.getRag() != null ? properties.getRag().getTopK() : null;
            return new RagPipeline(chat, documentStore, topK != null ? topK : 3);
        }
    }

    @Configuration
    @ConditionalOnClass(Agent.class)
    static class AgentAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(Agent.class)
        public Agent defaultAgent(AiChat chat) {
            return new Agent("default", "你是一个智能助手，能够使用各种工具完成任务", chat);
        }

        @Bean
        public static AiAgentRegistrar aiAgentRegistrar() {
            return new AiAgentRegistrar();
        }
    }

    @Configuration
    @ConditionalOnClass(AiTool.class)
    static class ToolAutoConfiguration {

        @Bean
        public ToolRegistrar toolRegistrar() {
            return new ToolRegistrar();
        }
    }

    /**
     * 扫描 @AiAgent 注解的 Bean：
     * 容器启动完成后，为每个注解类创建 Agent（注入 AiChat，
     * 类内 @AiTool 方法自动注册为工具），并以 "thinkAi4jAgent:名称" 注册为单例。
     */
    public static class AiAgentRegistrar implements BeanPostProcessor, BeanFactoryAware, SmartInitializingSingleton {

        private final List<Object> annotatedBeans = new CopyOnWriteArrayList<>();
        private ConfigurableListableBeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            if (beanFactory instanceof ConfigurableListableBeanFactory configurable) {
                this.beanFactory = configurable;
            }
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean != null && AnnotationUtils.findAnnotation(bean.getClass(), AiAgent.class) != null) {
                annotatedBeans.add(bean);
            }
            return bean;
        }

        @Override
        public void afterSingletonsInstantiated() {
            if (beanFactory == null || annotatedBeans.isEmpty()) {
                return;
            }
            AiChat chat;
            try {
                chat = beanFactory.getBean(AiChat.class);
            } catch (Exception e) {
                return;
            }
            for (Object bean : annotatedBeans) {
                AiAgent annotation = AnnotationUtils.findAnnotation(bean.getClass(), AiAgent.class);
                if (annotation == null) {
                    continue;
                }
                String name = annotation.name().isEmpty() ? bean.getClass().getSimpleName() : annotation.name();
                String systemPrompt = annotation.description().isEmpty()
                        ? "你是一个智能助手，能够使用各种工具完成任务"
                        : annotation.description();
                Agent agent = new Agent(name, systemPrompt, chat);
                // 注解类中的 @AiTool 方法自动注册为该 Agent 的工具
                agent.addToolBean(bean);
                String agentBeanName = "thinkAi4jAgent:" + name;
                if (!beanFactory.containsSingleton(agentBeanName)) {
                    beanFactory.registerSingleton(agentBeanName, agent);
                }
            }
        }
    }

    public static class ToolRegistrar {
        public void registerToolBeans(Agent agent, Map<String, Object> beans) {
            for (Object bean : beans.values()) {
                registerToolBeans(agent, bean);
            }
        }

        public void registerToolBeans(Agent agent, Object bean) {
            Method[] methods = bean.getClass().getDeclaredMethods();
            for (Method method : methods) {
                if (method.getAnnotation(AiTool.class) != null) {
                    agent.getToolExecutor().register(bean);
                    break;
                }
            }
        }
    }
}

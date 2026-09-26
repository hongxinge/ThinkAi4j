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

    @Bean
    @ConditionalOnMissingBean
    public ChatProviderRegistry chatProviderRegistry(ThinkAiProperties properties) {
        ChatProviderRegistry registry = new ChatProviderRegistry();
        registry.setDefaultProvider(properties.getDefaultProvider());
        return registry;
    }

    @Configuration
    @ConditionalOnClass(OpenAiCompatProvider.class)
    @ConditionalOnProperty(prefix = "think.ai.compat", name = "providers")
    static class OpenAiCompatAutoConfiguration {

        @Bean
        public List<ChatProvider> openAiCompatProviders(OpenAiCompatConfig config) {
            return config.getProviders().stream()
                .<ChatProvider>map(providerConfig -> new OpenAiCompatProvider(providerConfig, config.getHttpClient()))
                .toList();
        }

        @Bean
        public ChatProvider openAiCompatRegistrar(List<ChatProvider> providers, ChatProviderRegistry registry) {
            for (ChatProvider provider : providers) {
                registry.registerProvider(provider);
                if (registry.getDefaultProvider() == null) {
                    registry.setDefaultProvider(provider.getProviderName());
                }
            }
            return providers.isEmpty() ? null : providers.get(0);
        }
    }

    @Bean
    @ConditionalOnMissingBean(AiChat.class)
    public AiChat aiChat(ChatProviderRegistry registry, Optional<ChatMemory> chatMemory,
                         ObjectProvider<AiMetricsCollector> metricsCollectorProvider) {
        DefaultAiChat chat = new DefaultAiChat(registry, chatMemory.orElse(null));
        AiMetricsCollector metrics = metricsCollectorProvider.getIfAvailable();
        // classpath 存在 Micrometer 时自动包装指标采集装饰器
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
    @ConditionalOnClass(ChatMemory.class)
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
    static class RagAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(DocumentStore.class)
        public DocumentStore documentStore() {
            return new InMemoryDocumentStore();
        }

        @Bean
        @ConditionalOnMissingBean(RagPipeline.class)
        public RagPipeline ragPipeline(AiChat chat, DocumentStore documentStore) {
            return new RagPipeline(chat, documentStore);
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

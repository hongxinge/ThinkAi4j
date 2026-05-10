package com.thinkai4j.spring.boot.starter;

import com.thinkai4j.provider.compat.OpenAiCompatConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "think.ai")
public class ThinkAiProperties {

    private String defaultProvider;

    private OpenAiCompatConfig compat = new OpenAiCompatConfig();

    private MemoryProperties memory = new MemoryProperties();

    private RagProperties rag = new RagProperties();

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public OpenAiCompatConfig getCompat() {
        return compat;
    }

    public void setCompat(OpenAiCompatConfig compat) {
        this.compat = compat;
    }

    public MemoryProperties getMemory() {
        return memory;
    }

    public void setMemory(MemoryProperties memory) {
        this.memory = memory;
    }

    public RagProperties getRag() {
        return rag;
    }

    public void setRag(RagProperties rag) {
        this.rag = rag;
    }

    public static class MemoryProperties {
        private String type;
        private Boolean enabled;
        private Integer maxMessages;
        private Long ttlMinutes;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getMaxMessages() {
            return maxMessages;
        }

        public void setMaxMessages(Integer maxMessages) {
            this.maxMessages = maxMessages;
        }

        public Long getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(Long ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }
    }

    public static class RagProperties {
        private Boolean enabled;
        private Integer topK;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }
    }
}

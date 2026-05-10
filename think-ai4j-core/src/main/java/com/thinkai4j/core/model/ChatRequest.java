package com.thinkai4j.core.model;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {

    private String model;
    private List<AiMessage> messages;
    private Double temperature;
    private Integer maxTokens;
    private List<String> stop;
    private Boolean stream;
    private List<ToolDefinition> tools;

    public ChatRequest() {
        this.messages = new ArrayList<>();
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<AiMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<AiMessage> messages) {
        this.messages = messages;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public List<String> getStop() {
        return stop;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public List<ToolDefinition> getTools() {
        return tools;
    }

    public void setTools(List<ToolDefinition> tools) {
        this.tools = tools;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ChatRequest request = new ChatRequest();

        public Builder model(String model) {
            request.setModel(model);
            return this;
        }

        public Builder message(String content) {
            request.getMessages().add(AiMessage.user(content));
            return this;
        }

        public Builder system(String content) {
            request.getMessages().add(AiMessage.system(content));
            return this;
        }

        public Builder messages(List<AiMessage> messages) {
            request.setMessages(messages);
            return this;
        }

        public Builder temperature(Double temperature) {
            request.setTemperature(temperature);
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            request.setMaxTokens(maxTokens);
            return this;
        }

        public Builder stream(boolean stream) {
            request.setStream(stream);
            return this;
        }

        public ChatRequest build() {
            return request;
        }
    }
}

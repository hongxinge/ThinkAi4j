package com.thinkai4j.core.model;

public class AiResponse {

    private String content;
    private Usage usage;
    private String model;
    private String finishReason;
    private java.util.List<ToolCall> toolCalls;

    public AiResponse() {
    }

    public AiResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public java.util.List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(java.util.List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }
}

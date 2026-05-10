package com.thinkai4j.core.model;

import java.util.List;

public class AiMessage {

    private MessageType role;
    private String content;
    private String name;
    private List<ToolCall> toolCalls;
    private String toolCallId;

    public AiMessage() {
    }

    public AiMessage(MessageType role, String content) {
        this.role = role;
        this.content = content;
    }

    public static AiMessage system(String content) {
        return new AiMessage(MessageType.SYSTEM, content);
    }

    public static AiMessage user(String content) {
        return new AiMessage(MessageType.USER, content);
    }

    public static AiMessage assistant(String content) {
        return new AiMessage(MessageType.ASSISTANT, content);
    }

    public MessageType getRole() {
        return role;
    }

    public void setRole(MessageType role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }
}

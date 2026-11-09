package com.thinkai4j.agent;

import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.MessageType;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.tool.ToolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 框架 - 支持多轮工具调用、ReAct 推理模式、记忆管理
 * 
 * 执行流程：
 * 1. 接收用户任务
 * 2. 推理（调用 LLM 决定下一步动作）
 * 3. 行动（调用工具）
 * 4. 观察（收集工具结果）
 * 5. 重复 2-4 直到得出最终答案
 */
public class Agent {

    private static final Logger log = LoggerFactory.getLogger(Agent.class);

    private final String name;
    private final String systemPrompt;
    private final AiChat chat;
    private final ToolExecutor toolExecutor;
    private ChatMemory memory;
    private String conversationId;
    private AgentLongTermMemory longTermMemory;
    private final List<AiMessage> messages = new CopyOnWriteArrayList<>();
    private int maxIterations = 10;

    public Agent(String name, String systemPrompt, AiChat chat) {
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.chat = chat;
        this.toolExecutor = new ToolExecutor();
    }

    public Agent addToolBean(Object bean) {
        toolExecutor.register(bean);
        return this;
    }

    public Agent memory(ChatMemory memory, String conversationId) {
        this.memory = memory;
        this.conversationId = conversationId;
        return this;
    }

    public Agent longTermMemory(AgentLongTermMemory longTermMemory) {
        this.longTermMemory = longTermMemory;
        return this;
    }

    public Agent maxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    /**
     * 执行任务 - 支持多轮工具调用的 Agent 核心循环
     */
    public String execute(String task) {
        List<AiMessage> history = buildHistory(task);
        // 仅把本轮新增消息写回记忆：history 末尾的 user(task) 及其后追加的消息。
        // 此前会重复写入 user(task)，并把从记忆加载的历史再次回写。
        int newMessageStart = Math.max(0, history.size() - 1);

        for (int i = 0; i < maxIterations; i++) {
            log.debug("Agent [{}] iteration {}/{}", name, i + 1, maxIterations);

            ChatRequest request = ChatRequest.builder()
                    .messages(new ArrayList<>(history))
                    .tools(getToolDefinitions())
                    .build();

            AiResponse response = chat.chat(request);
            if (response == null) {
                log.warn("Agent [{}] received null response at iteration {}", name, i + 1);
                break;
            }

            // 工具调用响应的 content 通常为 null，必须先处理 tool_calls，不能提前 break；
            // 且 assistant 消息需携带 tool_calls，才能与后续 tool 结果正确配对。
            if (response.getToolCalls() != null && !response.getToolCalls().isEmpty()) {
                history.add(createAssistantMessageWithToolCalls(response));
                for (ToolCall toolCall : response.getToolCalls()) {
                    String toolName = toolCall.getFunction().getName();
                    log.debug("Agent [{}] calling tool: {}", name, toolName);

                    String result = executeToolSafely(toolCall);
                    history.add(createToolResultMessage(toolCall, result));

                    if (longTermMemory != null && (toolName.contains("remember") || toolName.contains("save"))) {
                        longTermMemory.rememberFact(result);
                    }
                }
                continue;
            }

            if (response.getContent() == null) {
                log.warn("Agent [{}] received empty content at iteration {}", name, i + 1);
                break;
            }

            history.add(AiMessage.assistant(response.getContent()));
            if (longTermMemory != null) {
                longTermMemory.addShortTermMessage(AiMessage.user(task));
                longTermMemory.addShortTermMessage(AiMessage.assistant(response.getContent()));
            }

            saveToMemory(history, newMessageStart);
            return response.getContent();
        }

        saveToMemory(history, newMessageStart);
        throw new AiException("Agent [" + name + "] exceeded max iterations (" + maxIterations + ") with no final answer");
    }

    private List<AiMessage> buildHistory(String task) {
        List<AiMessage> history = new ArrayList<>();
        
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            history.add(AiMessage.system(systemPrompt));
        }

        if (longTermMemory != null) {
            String context = longTermMemory.getSystemContext();
            if (!context.isEmpty()) {
                history.add(AiMessage.system(context));
            }
            List<AiMessage> shortTermHistory = longTermMemory.getShortTermHistory();
            history.addAll(shortTermHistory);
        } else if (conversationId != null && memory != null) {
            List<AiMessage> memMessages = memory.getMessages(conversationId);
            if (memMessages != null && !memMessages.isEmpty()) {
                history.addAll(memMessages);
            }
        }

        history.addAll(messages);
        history.add(AiMessage.user(task));
        
        return history;
    }

    private List<ToolDefinition> getToolDefinitions() {
        return toolExecutor.getToolDefinitions();
    }

    private String executeToolSafely(ToolCall toolCall) {
        try {
            return toolExecutor.execute(toolCall);
        } catch (Exception e) {
            log.error("Tool execution failed for agent [{}]: {}", name, e.getMessage());
            return "工具执行失败: " + e.getMessage();
        }
    }

    private AiMessage createToolResultMessage(ToolCall toolCall, String result) {
        AiMessage message = new AiMessage(MessageType.TOOL, result);
        message.setToolCallId(toolCall.getId());
        message.setName(toolCall.getFunction().getName());
        return message;
    }

    private void saveToMemory(List<AiMessage> history, int newMessageStart) {
        if (conversationId == null || memory == null) {
            return;
        }
        for (int i = newMessageStart; i < history.size(); i++) {
            memory.addMessage(conversationId, history.get(i));
        }
    }

    private AiMessage createAssistantMessageWithToolCalls(AiResponse response) {
        AiMessage message = new AiMessage(MessageType.ASSISTANT, response.getContent());
        message.setToolCalls(response.getToolCalls());
        return message;
    }

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    public String getName() {
        return name;
    }
}

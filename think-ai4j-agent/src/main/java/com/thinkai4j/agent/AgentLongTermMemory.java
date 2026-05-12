package com.thinkai4j.agent;

import com.thinkai4j.core.memory.ChatMemory;
import com.thinkai4j.core.model.AiMessage;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Agent 长期记忆 - 支持跨会话记忆和关键信息提取
 * 分为短期记忆（当前对话）和长期记忆（持久化关键信息）
 */
public class AgentLongTermMemory {

    private final ChatMemory shortTermMemory;
    private final List<String> longTermFacts = new CopyOnWriteArrayList<>();
    private final String agentId;

    public AgentLongTermMemory(String agentId, ChatMemory shortTermMemory) {
        this.agentId = agentId;
        this.shortTermMemory = shortTermMemory;
    }

    /**
     * 保存短期消息到对话记忆
     */
    public void addShortTermMessage(AiMessage message) {
        if (shortTermMemory != null) {
            shortTermMemory.addMessage(agentId, message);
        }
    }

    /**
     * 获取短期对话历史
     */
    public List<AiMessage> getShortTermHistory() {
        if (shortTermMemory == null) {
            return List.of();
        }
        return shortTermMemory.getMessages(agentId);
    }

    /**
     * 记住一个长期事实（关键信息）
     */
    public void rememberFact(String fact) {
        if (fact != null && !fact.isEmpty()) {
            longTermFacts.add(fact);
        }
    }

    /**
     * 获取所有长期记忆的事实
     */
    public List<String> getLongTermFacts() {
        return List.copyOf(longTermFacts);
    }

    /**
     * 搜索长期记忆中的相关事实
     */
    public List<String> searchFacts(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return getLongTermFacts();
        }
        String lowerKeyword = keyword.toLowerCase();
        return longTermFacts.stream()
                .filter(fact -> fact.toLowerCase().contains(lowerKeyword))
                .toList();
    }

    /**
     * 清除所有长期记忆
     */
    public void clearLongTermMemory() {
        longTermFacts.clear();
    }

    /**
     * 清除短期对话历史
     */
    public void clearShortTermMemory() {
        if (shortTermMemory != null) {
            shortTermMemory.clear(agentId);
        }
    }

    /**
     * 获取完整的系统上下文（长期记忆摘要）
     */
    public String getSystemContext() {
        if (longTermFacts.isEmpty()) {
            return "";
        }
        return "长期记忆中的关键信息:\n" + String.join("\n", longTermFacts);
    }
}

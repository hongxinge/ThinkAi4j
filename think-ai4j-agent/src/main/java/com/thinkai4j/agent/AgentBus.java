package com.thinkai4j.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 总线 - 支持多 Agent 之间的通信和任务分配
 * Agent 可以注册到总线上，通过总线进行消息传递和任务委托
 */
public class AgentBus {

    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * 注册 Agent 到总线
     */
    public void register(String name, Agent agent) {
        agents.put(name, agent);
    }

    /**
     * 从总线移除 Agent
     */
    public void unregister(String name) {
        agents.remove(name);
    }

    /**
     * 获取注册的 Agent
     */
    public Agent getAgent(String name) {
        return agents.get(name);
    }

    /**
     * 获取所有注册的 Agent 名称
     */
    public List<String> getAgentNames() {
        return agents.keySet().stream().toList();
    }

    /**
     * 通过总线调用指定 Agent 执行任务
     */
    public String execute(String agentName, String task) {
        Agent agent = agents.get(agentName);
        if (agent == null) {
            return "Agent not found: " + agentName;
        }
        return agent.execute(task);
    }

    /**
     * 多 Agent 协作执行 - 按顺序执行任务
     * 每个 Agent 的输出作为下一个 Agent 的输入
     */
    public String chainExecute(List<String> agentNames, String initialTask) {
        String result = initialTask;
        for (String agentName : agentNames) {
            Agent agent = agents.get(agentName);
            if (agent == null) {
                return "Agent not found in chain: " + agentName;
            }
            result = agent.execute(result);
        }
        return result;
    }

    /**
     * 多 Agent 协作执行 - 并行执行任务，汇总结果
     * 各 Agent 在独立线程中同时执行，全部完成后按提交顺序汇总输出
     */
    public String parallelExecute(Map<String, String> agentTasks) {
        if (agentTasks == null || agentTasks.isEmpty()) {
            return "";
        }

        Map<String, CompletableFuture<String>> futures = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : agentTasks.entrySet()) {
            String agentName = entry.getKey();
            String task = entry.getValue();
            Agent agent = agents.get(agentName);
            if (agent == null) {
                futures.put(agentName, CompletableFuture.completedFuture(null));
            } else {
                futures.put(agentName, CompletableFuture.supplyAsync(() -> agent.execute(task)));
            }
        }

        StringBuilder results = new StringBuilder();
        for (Map.Entry<String, CompletableFuture<String>> entry : futures.entrySet()) {
            String agentName = entry.getKey();
            CompletableFuture<String> future = entry.getValue();
            if (agents.get(agentName) == null) {
                results.append("Agent not found: ").append(agentName).append("\n");
                continue;
            }
            String result;
            try {
                result = future.join();
            } catch (CompletionException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                result = "Agent execution failed: " + cause.getMessage();
            }
            results.append("[").append(agentName).append("]: ").append(result).append("\n");
        }
        return results.toString();
    }
}

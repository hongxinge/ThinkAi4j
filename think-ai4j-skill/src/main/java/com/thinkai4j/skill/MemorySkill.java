package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记忆 Skill - 为 Agent 提供键值对记忆能力
 * 支持简单的 KV 存储，可在对话中记住关键信息
 */
public class MemorySkill {

    private final Map<String, String> memory = new ConcurrentHashMap<>();

    @AiTool("记住一个信息")
    public String remember(
            @ToolParam(description = "记忆的键（如：用户名）") String key,
            @ToolParam(description = "记忆的值（如：张三）") String value) {
        memory.put(key, value);
        return "已记住: " + key + " = " + value;
    }

    @AiTool("查询记住的信息")
    public String recall(
            @ToolParam(description = "记忆的键（如：用户名），不传则返回所有记忆") String key) {
        if (key == null || key.isEmpty()) {
            if (memory.isEmpty()) {
                return "当前没有记住任何信息";
            }
            return "当前记忆:\n" + memory.entrySet().stream()
                    .map(e -> "- " + e.getKey() + ": " + e.getValue())
                    .reduce("", (a, b) -> a + "\n" + b);
        }
        String value = memory.get(key);
        if (value == null) {
            return "没有记住键 " + key + " 的信息";
        }
        return key + " = " + value;
    }

    @AiTool("忘记一个信息")
    public String forget(
            @ToolParam(description = "要忘记的键") String key) {
        if (memory.remove(key) != null) {
            return "已忘记: " + key;
        }
        return "没有找到键: " + key;
    }

    @AiTool("清空所有记忆")
    public String clearMemory() {
        int size = memory.size();
        memory.clear();
        return "已清空 " + size + " 条记忆";
    }
}

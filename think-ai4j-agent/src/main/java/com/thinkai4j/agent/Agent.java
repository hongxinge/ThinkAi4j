package com.thinkai4j.agent;

import com.thinkai4j.core.api.AiChat;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.MessageType;
import com.thinkai4j.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.List;

public class Agent {

    private final String name;
    private final String systemPrompt;
    private final AiChat chat;
    private final ToolExecutor toolExecutor;

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

    public String execute(String task) {
        String input = systemPrompt != null && !systemPrompt.isEmpty()
                ? systemPrompt + "\n\n用户任务：" + task
                : task;
        return chat.system(systemPrompt).ask(input);
    }

    public ToolExecutor getToolExecutor() {
        return toolExecutor;
    }

    public String getName() {
        return name;
    }
}

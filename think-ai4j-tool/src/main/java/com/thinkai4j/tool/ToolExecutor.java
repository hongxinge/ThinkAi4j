package com.thinkai4j.tool;

import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

public class ToolExecutor {

    private final Map<String, ToolInstance> tools = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void register(Object bean, Method method, String description) {
        String name = method.getName();
        tools.put(name, new ToolInstance(bean, method, description));
    }

    public void register(Object bean) {
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            AiTool toolAnn = method.getAnnotation(AiTool.class);
            if (toolAnn != null) {
                String desc = toolAnn.value().isEmpty() ? toolAnn.name() : toolAnn.value();
                register(bean, method, desc);
            }
        }
    }

    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> definitions = new ArrayList<>();

        for (Map.Entry<String, ToolInstance> entry : tools.entrySet()) {
            ToolInstance instance = entry.getValue();
            ToolDefinition definition = new ToolDefinition();

            ToolDefinition.FunctionDefinition function = new ToolDefinition.FunctionDefinition();
            function.setName(entry.getKey());
            function.setDescription(instance.description);

            Parameter[] parameters = instance.method.getParameters();
            for (Parameter param : parameters) {
                ToolParam toolParam = param.getAnnotation(ToolParam.class);
                String desc = toolParam != null ? toolParam.description() : param.getName();
                boolean required = toolParam == null || toolParam.required();
                function.addParameter(param.getName(), getTypeName(param.getType()), desc, required);
            }

            definition.setFunction(function);
            definitions.add(definition);
        }

        return definitions;
    }

    public String execute(ToolCall toolCall) {
        String name = toolCall.getFunction().getName();
        String argumentsJson = toolCall.getFunction().getArguments();

        ToolInstance instance = tools.get(name);
        if (instance == null) {
            throw new AiException("Tool not found: " + name);
        }

        try {
            JsonNode argsNode = objectMapper.readTree(argumentsJson);
            Parameter[] parameters = instance.method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName();
                JsonNode valueNode = argsNode.get(paramName);
                args[i] = convertValue(valueNode, parameters[i].getType());
            }

            Object result = instance.method.invoke(instance.bean, args);
            return result != null ? result.toString() : "";

        } catch (Exception e) {
            throw new AiException("tool", "EXECUTION_ERROR", "Failed to execute tool: " + name, e);
        }
    }

    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    private String getTypeName(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class) return "integer";
        if (type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class) return "number";
        if (type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type == List.class) return "array";
        return "string";
    }

    private Object convertValue(JsonNode node, Class<?> type) {
        if (node == null || node.isNull()) return null;
        if (type == String.class) return node.asText();
        if (type == int.class || type == Integer.class) return node.asInt();
        if (type == long.class || type == Long.class) return node.asLong();
        if (type == double.class || type == Double.class) return node.asDouble();
        if (type == float.class || type == Float.class) return (float) node.asDouble();
        if (type == boolean.class || type == Boolean.class) return node.asBoolean();
        return node.asText();
    }

    private static class ToolInstance {
        final Object bean;
        final Method method;
        final String description;

        ToolInstance(Object bean, Method method, String description) {
            this.bean = bean;
            this.method = method;
            this.description = description;
        }
    }
}

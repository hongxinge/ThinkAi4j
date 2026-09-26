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
import java.util.concurrent.ConcurrentHashMap;

public class ToolExecutor {

    private final Map<String, ToolInstance> tools = new ConcurrentHashMap<>();
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
            JsonNode argsNode = objectMapper.readTree(argumentsJson != null ? argumentsJson : "{}");
            Parameter[] parameters = instance.method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName();
                JsonNode valueNode = argsNode.get(paramName);
                if (valueNode == null) {
                    valueNode = argsNode.get("arg" + i);
                }
                args[i] = convertValue(valueNode, paramName, parameters[i].getType());
            }

            Object result = instance.method.invoke(instance.bean, args);
            return result != null ? result.toString() : "";

        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            // 解包反射调用异常，暴露工具方法抛出的真实异常信息
            Throwable cause = (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null)
                    ? e.getCause() : e;
            throw new AiException("tool", "EXECUTION_ERROR",
                    "Failed to execute tool: " + name + ": " + cause.getMessage(), cause);
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
        if (type == List.class || type.isArray()) return "array";
        if (type == Map.class || !type.isPrimitive()) return "object";
        return "string";
    }

    private Object convertValue(JsonNode node, String paramName, Class<?> type) {
        if (node == null || node.isNull()) {
            if (type.isPrimitive()) {
                // 原生类型参数不允许缺失（无法传 null），给出明确错误便于大模型自行修正
                throw new AiException("tool", "MISSING_PARAMETER",
                        "Missing required parameter '" + paramName + "' of primitive type " + type.getName());
            }
            return null;
        }
        if (type == String.class) return node.asText();
        if (type == int.class || type == Integer.class) return node.asInt();
        if (type == long.class || type == Long.class) return node.asLong();
        if (type == double.class || type == Double.class) return node.asDouble();
        if (type == float.class || type == Float.class) return (float) node.asDouble();
        if (type == boolean.class || type == Boolean.class) return node.asBoolean();
        // 复杂类型（POJO/List/Map 等）交由 Jackson 反序列化
        try {
            return objectMapper.treeToValue(node, type);
        } catch (Exception e) {
            throw new AiException("tool", "PARAM_CONVERT_ERROR",
                    "Failed to convert parameter '" + paramName + "' to " + type.getName() + ": " + e.getMessage(), e);
        }
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

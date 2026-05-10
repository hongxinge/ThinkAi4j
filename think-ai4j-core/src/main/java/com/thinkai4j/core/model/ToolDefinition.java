package com.thinkai4j.core.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ToolDefinition {

    private String type;
    private FunctionDefinition function;

    public ToolDefinition() {
        this.type = "function";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FunctionDefinition getFunction() {
        return function;
    }

    public void setFunction(FunctionDefinition function) {
        this.function = function;
    }

    public static class FunctionDefinition {

        private String name;
        private String description;
        private Map<String, Object> parameters;

        public FunctionDefinition() {
            this.parameters = new LinkedHashMap<>();
            this.parameters.put("type", "object");
            this.parameters.put("properties", new LinkedHashMap<String, Object>());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        @SuppressWarnings("unchecked")
        public FunctionDefinition addParameter(String name, String type, String description, boolean required) {
            Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
            Map<String, Object> param = new LinkedHashMap<>();
            param.put("type", type);
            param.put("description", description);
            properties.put(name, param);

            if (required) {
                java.util.List<String> requiredList = (java.util.List<String>) parameters.computeIfAbsent("required", k -> new java.util.ArrayList<String>());
                requiredList.add(name);
            }
            return this;
        }
    }
}

package com.thinkai4j.provider.compat;

import com.thinkai4j.core.api.ChatProvider;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.model.Usage;
import com.thinkai4j.core.exception.AiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class OpenAiCompatProvider implements ChatProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatProvider.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final String providerName;
    private final String baseUrl;
    private final String defaultModel;
    private final String apiKey;
    private final Map<String, String> customHeaders;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiCompatProvider(OpenAiCompatConfig.ModelProvider config, OpenAiCompatConfig.HttpClientConfig httpClientConfig) {
        this.providerName = config.getName();
        this.baseUrl = config.getBaseUrl();
        this.defaultModel = config.getModel();
        this.apiKey = config.getApiKey();
        this.customHeaders = config.getHeaders();

        ConnectionPool connectionPool = new ConnectionPool(
                httpClientConfig.getConnectionPool().getMaxIdleConnections(),
                httpClientConfig.getConnectionPool().getKeepAliveMinutes(),
                TimeUnit.MINUTES
        );

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .connectTimeout(httpClientConfig.getTimeout().getConnectSeconds(), TimeUnit.SECONDS)
                .readTimeout(httpClientConfig.getTimeout().getReadSeconds(), TimeUnit.SECONDS)
                .writeTimeout(httpClientConfig.getTimeout().getWriteSeconds(), TimeUnit.SECONDS);

        this.httpClient = clientBuilder.build();
        this.objectMapper = new ObjectMapper();

        log.info("Initialized OpenAI-compatible provider: {} (baseUrl: {})", providerName, baseUrl);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public AiResponse chat(ChatRequest request) {
        try {
            String requestBody = buildRequestBody(request, false);
            Request httpRequest = buildRequest(requestBody);

            try (Response response = httpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new AiException(providerName, "HTTP_" + response.code(),
                            providerName + " API error: " + response.code() + " " + response.message());
                }
                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            throw new AiException(providerName, "REQUEST_ERROR", "Failed to call " + providerName + " API", e);
        }
    }

    @Override
    public Flux<String> stream(ChatRequest request) {
        return Flux.create(sink -> {
            try {
                String requestBody = buildRequestBody(request, true);
                Request httpRequest = buildRequest(requestBody);

                EventSourceListener listener = new EventSourceListener() {
                    @Override
                    public void onEvent(@NotNull EventSource eventSource, String id, String type, @NotNull String data) {
                        if ("[DONE]".equals(data)) {
                            sink.complete();
                            return;
                        }
                        try {
                            JsonNode jsonNode = objectMapper.readTree(data);
                            JsonNode choices = jsonNode.get("choices");
                            if (choices != null && choices.isArray() && choices.size() > 0) {
                                JsonNode delta = choices.get(0).get("delta");
                                if (delta != null && delta.has("content")) {
                                    String content = delta.get("content").asText();
                                    if (content != null && !content.isEmpty()) {
                                        sink.next(content);
                                    }
                                }
                                JsonNode finishReason = choices.get(0).get("finish_reason");
                                if (finishReason != null && !finishReason.isNull()) {
                                    sink.complete();
                                }
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse SSE event from provider {}: {}", providerName, e.getMessage());
                            sink.error(new AiException(providerName, "PARSE_ERROR", "Failed to parse SSE event", e));
                        }
                    }

                    @Override
                    public void onFailure(@NotNull EventSource eventSource, Throwable t, Response response) {
                        String errorMsg = (response != null) 
                                ? "HTTP " + response.code() + ": " + response.message() 
                                : t.getMessage();
                        log.error("Stream failed for provider {}: {}", providerName, errorMsg, t);
                        sink.error(new AiException(providerName, "STREAM_ERROR", "Stream failed: " + errorMsg, t));
                    }

                    @Override
                    public void onClosed(@NotNull EventSource eventSource) {
                        sink.complete();
                    }
                };

                EventSources.createFactory(httpClient).newEventSource(httpRequest, listener);

            } catch (Exception e) {
                log.error("Failed to setup stream for provider {}: {}", providerName, e.getMessage(), e);
                sink.error(new AiException(providerName, "SETUP_ERROR", "Failed to setup stream", e));
            }
        });
    }

    private String buildRequestBody(ChatRequest request, boolean stream) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("model", request.getModel() != null ? request.getModel() : defaultModel);
        rootNode.put("stream", stream);

        if (request.getTemperature() != null) {
            rootNode.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            rootNode.put("max_tokens", request.getMaxTokens());
        }

        ArrayNode messagesNode = objectMapper.createArrayNode();
        for (AiMessage message : request.getMessages()) {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("role", message.getRole().name().toLowerCase());
            messageNode.put("content", message.getContent());

            if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
                ArrayNode toolCallsNode = objectMapper.createArrayNode();
                for (ToolCall tc : message.getToolCalls()) {
                    ObjectNode toolCallNode = objectMapper.createObjectNode();
                    toolCallNode.put("id", tc.getId());
                    toolCallNode.put("type", tc.getType());
                    ObjectNode functionNode = objectMapper.createObjectNode();
                    functionNode.put("name", tc.getFunction().getName());
                    functionNode.put("arguments", tc.getFunction().getArguments());
                    toolCallNode.set("function", functionNode);
                    toolCallsNode.add(toolCallNode);
                }
                messageNode.set("tool_calls", toolCallsNode);
            }
            if (message.getToolCallId() != null) {
                messageNode.put("tool_call_id", message.getToolCallId());
            }
            messagesNode.add(messageNode);
        }
        rootNode.set("messages", messagesNode);

        if (request.getTools() != null && !request.getTools().isEmpty()) {
            ArrayNode toolsNode = objectMapper.createArrayNode();
            for (ToolDefinition tool : request.getTools()) {
                ObjectNode toolNode = objectMapper.createObjectNode();
                toolNode.put("type", tool.getType());
                ObjectNode functionNode = objectMapper.createObjectNode();
                functionNode.put("name", tool.getFunction().getName());
                functionNode.put("description", tool.getFunction().getDescription());
                functionNode.set("parameters", objectMapper.valueToTree(tool.getFunction().getParameters()));
                toolNode.set("function", functionNode);
                toolsNode.add(toolNode);
            }
            rootNode.set("tools", toolsNode);
        }

        return objectMapper.writeValueAsString(rootNode);
    }

    private Request buildRequest(String body) {
        String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON));

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }

        if (customHeaders != null) {
            customHeaders.forEach(builder::header);
        }

        return builder.build();
    }

    private AiResponse parseResponse(String responseBody) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        AiResponse response = new AiResponse();

        JsonNode choices = jsonNode.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode message = choices.get(0).get("message");
            if (message != null) {
                JsonNode contentNode = message.get("content");
                if (contentNode != null && !contentNode.isNull()) {
                    response.setContent(contentNode.asText());
                }

                if (message.has("tool_calls") && message.get("tool_calls").isArray()) {
                    List<ToolCall> toolCalls = new ArrayList<>();
                    for (JsonNode tc : message.get("tool_calls")) {
                        ToolCall toolCall = new ToolCall();
                        toolCall.setId(tc.get("id").asText());
                        toolCall.setType(tc.get("type").asText());
                        ToolCall.FunctionCall function = new ToolCall.FunctionCall();
                        JsonNode funcNode = tc.get("function");
                        if (funcNode != null) {
                            function.setName(funcNode.get("name").asText());
                            function.setArguments(funcNode.get("arguments").asText());
                        }
                        toolCall.setFunction(function);
                        toolCalls.add(toolCall);
                    }
                    response.setToolCalls(toolCalls);
                }
            }
            JsonNode finishReason = choices.get(0).get("finish_reason");
            if (finishReason != null) {
                response.setFinishReason(finishReason.asText());
            }
        }

        JsonNode usage = jsonNode.get("usage");
        if (usage != null) {
            response.setUsage(new Usage(
                    usage.path("prompt_tokens").asInt(0),
                    usage.path("completion_tokens").asInt(0),
                    usage.path("total_tokens").asInt(0)
            ));
        }

        JsonNode modelNode = jsonNode.get("model");
        if (modelNode != null) {
            response.setModel(modelNode.asText());
        }

        return response;
    }
}

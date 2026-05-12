package com.thinkai4j.test;

import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.provider.compat.OpenAiCompatConfig;
import com.thinkai4j.provider.compat.OpenAiCompatProvider;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiCompatProviderTest {

    private OpenAiCompatProvider createProvider(String baseUrl, String apiKey, String model) {
        OpenAiCompatConfig.ModelProvider modelProvider = new OpenAiCompatConfig.ModelProvider();
        modelProvider.setName("test-provider");
        modelProvider.setBaseUrl(baseUrl);
        modelProvider.setApiKey(apiKey);
        modelProvider.setModel(model != null ? model : "gpt-3.5-turbo");

        OpenAiCompatConfig config = new OpenAiCompatConfig();
        config.setProviders(List.of(modelProvider));

        OpenAiCompatConfig.HttpClientConfig httpClientConfig = new OpenAiCompatConfig.HttpClientConfig();
        config.setHttpClient(httpClientConfig);

        return new OpenAiCompatProvider(modelProvider, httpClientConfig);
    }

    @Test
    void testProviderName() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "test-key", null);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testChatWithInvalidApiKey() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid-key", "gpt-3.5-turbo");
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithInvalidBaseUrl() {
        OpenAiCompatProvider provider = createProvider("http://invalid.nonexistent.domain/api/", "", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .build();
        
        assertThrows(Exception.class, () -> provider.chat(request));
    }

    @Test
    void testStreamWithInvalidBaseUrl() {
        OpenAiCompatProvider provider = createProvider("http://invalid.nonexistent.domain/api/", "", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .build();
        
        Flux<String> flux = provider.stream(request);
        StepVerifier.create(flux)
                .expectError()
                .verify();
    }

    @Test
    void testBaseUrlWithTrailingSlash() {
        OpenAiCompatProvider provider = createProvider("https://api.example.com/v1/", "key", null);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testBaseUrlWithoutTrailingSlash() {
        OpenAiCompatProvider provider = createProvider("https://api.example.com/v1", "key", null);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testChatWithTemperature() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .temperature(0.7)
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithMaxTokens() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .maxTokens(100)
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithTools() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid", null);
        
        ToolDefinition.FunctionDefinition funcDef = new ToolDefinition.FunctionDefinition();
        funcDef.setName("get_weather");
        funcDef.setDescription("Get weather");

        ToolDefinition tool = new ToolDefinition();
        tool.setFunction(funcDef);

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("What's the weather?")))
                .tools(List.of(tool))
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testStreamRequest() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .build();
        
        Flux<String> flux = provider.stream(request);
        StepVerifier.create(flux)
                .expectError()
                .verify();
    }

    @Test
    void testMultipleMessages() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        AiMessage.system("You are a helpful assistant"),
                        AiMessage.user("Hello"),
                        AiMessage.assistant("Hi!"),
                        AiMessage.user("How are you?")
                ))
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithToolCallResponse() {
        OpenAiCompatProvider provider = createProvider("https://api.openai.com/v1/", "invalid", null);
        
        AiMessage assistantMsg = new AiMessage(com.thinkai4j.core.model.MessageType.ASSISTANT, null);
        ToolCall toolCall = new ToolCall();
        toolCall.setId("call_123");
        toolCall.setType("function");
        ToolCall.FunctionCall funcCall = new ToolCall.FunctionCall();
        funcCall.setName("get_weather");
        funcCall.setArguments("{\"location\": \"Beijing\"}");
        toolCall.setFunction(funcCall);
        assistantMsg.setToolCalls(List.of(toolCall));
        
        AiMessage toolResultMsg = new AiMessage(com.thinkai4j.core.model.MessageType.TOOL, "Sunny, 25°C");
        toolResultMsg.setToolCallId("call_123");
        toolResultMsg.setName("get_weather");
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        AiMessage.user("What's the weather?"),
                        assistantMsg,
                        toolResultMsg
                ))
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }
}

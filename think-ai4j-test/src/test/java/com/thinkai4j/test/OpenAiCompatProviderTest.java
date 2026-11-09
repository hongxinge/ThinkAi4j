package com.thinkai4j.test;

import com.thinkai4j.core.model.AiMessage;
import com.thinkai4j.core.model.AiResponse;
import com.thinkai4j.core.model.ChatRequest;
import com.thinkai4j.core.model.ToolCall;
import com.thinkai4j.core.model.ToolDefinition;
import com.thinkai4j.core.exception.AiException;
import com.thinkai4j.core.retry.RetryHandler;
import com.thinkai4j.provider.compat.OpenAiCompatConfig;
import com.thinkai4j.provider.compat.OpenAiCompatProvider;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAI 兼容 Provider 的离线回归测试。
 *
 * <p>原用例把真实外网地址作为 baseUrl,配合默认 3 次重试与 30/60 秒超时,
 * 单次构建要十几分钟且无法离线回归。这里改用 JDK 内置的 {@link HttpServer}
 * 在 127.0.0.1 上起一个本地桩服务:默认返回合法的 OpenAI 兼容 JSON,
 * 对携带无效凭证的请求按 Authorization 返回 401。所有用例仍然保持原有的
 * 断言语义,但完全离线、耗时极短。</p>
 */
class OpenAiCompatProviderTest {

    /** 被视为无效凭证的 Authorization 头,用于触发 401。 */
    private static final List<String> INVALID_AUTHORIZATIONS = List.of(
            "Bearer invalid", "Bearer invalid-key");

    /** 与 OpenAiCompatProvider.parseResponse 字段保持一致的成功响应体。 */
    private static final String SUCCESS_JSON = "{\n"
            + "  \"id\": \"chatcmpl-local\",\n"
            + "  \"object\": \"chat.completion\",\n"
            + "  \"model\": \"gpt-3.5-turbo\",\n"
            + "  \"choices\": [{\n"
            + "    \"index\": 0,\n"
            + "    \"message\": {\"role\": \"assistant\", \"content\": \"Hello from local test server\"},\n"
            + "    \"finish_reason\": \"stop\"\n"
            + "  }],\n"
            + "  \"usage\": {\"prompt_tokens\": 5, \"completion_tokens\": 8, \"total_tokens\": 13}\n"
            + "}";

    /** OpenAI 风格的错误响应体,配合 401 触发 AiException。 */
    private static final String ERROR_JSON = "{\n"
            + "  \"error\": {\"message\": \"Invalid API key\", \"type\": \"invalid_request_error\", \"code\": \"invalid_api_key\"}\n"
            + "}";

    private static HttpServer server;

    /** 本地服务地址,带尾斜杠,形如 http://127.0.0.1:端口/v1/。 */
    private static String localBaseUrl;

    /** 本地服务地址,不带尾斜杠,用于覆盖 baseUrl 拼接分支。 */
    private static String localBaseUrlNoSlash;

    /** 一个确定无人监听的本地地址,用于替代原来的无效域名。 */
    private static String deadBaseUrl;

    @BeforeAll
    static void startLocalServer() throws Exception {
        // 端口 0 由系统分配,再取回实际端口,避免端口冲突。
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", OpenAiCompatProviderTest::handle);
        server.setExecutor(null); // 使用默认轻量 executor,离线单测足够
        server.start();

        int port = server.getAddress().getPort();
        localBaseUrl = "http://127.0.0.1:" + port + "/v1/";
        localBaseUrlNoSlash = "http://127.0.0.1:" + port + "/v1";

        // 借系统分配得到一个空闲高位端口后立即释放,该地址确定没有服务监听,
        // 连接会被本机立刻拒绝,不会产生 DNS 查询或长超时。
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getByName("127.0.0.1"))) {
            deadBaseUrl = "http://127.0.0.1:" + socket.getLocalPort() + "/api/";
        }
    }

    @AfterAll
    static void stopLocalServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    /** 统一处理所有路径:默认返回合法 JSON,命中无效凭证或缺省凭证时返回 401。 */
    private static void handle(HttpExchange exchange) throws IOException {
        try {
            // 读取并丢弃请求体,避免 POST 数据未消费影响连接复用。
            try (InputStream in = exchange.getRequestBody()) {
                in.readAllBytes();
            }
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            boolean unauthorized = auth == null || auth.isEmpty()
                    || INVALID_AUTHORIZATIONS.contains(auth);
            int status = unauthorized ? 401 : 200;
            byte[] body = (unauthorized ? ERROR_JSON : SUCCESS_JSON).getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } finally {
            exchange.close();
        }
    }

    private OpenAiCompatProvider createProvider(String baseUrl, String apiKey, String model) {
        OpenAiCompatConfig.ModelProvider modelProvider = new OpenAiCompatConfig.ModelProvider();
        modelProvider.setName("test-provider");
        modelProvider.setBaseUrl(baseUrl);
        modelProvider.setApiKey(apiKey);
        modelProvider.setModel(model != null ? model : "gpt-3.5-turbo");

        OpenAiCompatConfig config = new OpenAiCompatConfig();
        config.setProviders(List.of(modelProvider));

        OpenAiCompatConfig.HttpClientConfig httpClientConfig = new OpenAiCompatConfig.HttpClientConfig();
        // 离线场景不需要长超时,进一步避免任何意外等待。
        httpClientConfig.getTimeout().setConnectSeconds(2);
        httpClientConfig.getTimeout().setReadSeconds(2);
        httpClientConfig.getTimeout().setWriteSeconds(2);
        config.setHttpClient(httpClientConfig);

        OpenAiCompatProvider provider = new OpenAiCompatProvider(modelProvider, httpClientConfig);
        speedUpRetries(provider);
        return provider;
    }

    /**
     * RetryHandler 默认把 Exception.class 也列为可重试异常,chat 一旦失败会重试 3 次
     * 并依次等待 1 秒、2 秒。为了保留"断言抛异常"的语义又不拖慢离线回归,
     * 这里仅取出同一个 RetryHandler 实例,把尝试次数与等待时间调到最小。
     * 只读取字段并修改对象状态,不改动任何生产代码文件。
     */
    private static void speedUpRetries(OpenAiCompatProvider provider) {
        try {
            Field field = OpenAiCompatProvider.class.getDeclaredField("retryHandler");
            field.setAccessible(true);
            RetryHandler retryHandler = (RetryHandler) field.get(provider);
            retryHandler.maxAttempts(1);
            retryHandler.initialDelayMs(0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法调整测试用重试参数", e);
        }
    }

    @Test
    void testProviderName() {
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "test-key", null);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testChatWithInvalidApiKey() {
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid-key", "gpt-3.5-turbo");
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithInvalidBaseUrl() {
        OpenAiCompatProvider provider = createProvider(deadBaseUrl, "", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .build();
        
        assertThrows(Exception.class, () -> provider.chat(request));
    }

    @Test
    void testStreamWithInvalidBaseUrl() {
        OpenAiCompatProvider provider = createProvider(deadBaseUrl, "", null);
        
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
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "key", null);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testBaseUrlWithoutTrailingSlash() {
        OpenAiCompatProvider provider = createProvider(localBaseUrlNoSlash, "key", null);
        assertEquals("test-provider", provider.getProviderName());
    }

    @Test
    void testChatWithTemperature() {
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .temperature(0.7)
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithMaxTokens() {
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid", null);
        
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(AiMessage.user("Hello")))
                .maxTokens(100)
                .build();
        
        assertThrows(AiException.class, () -> provider.chat(request));
    }

    @Test
    void testChatWithTools() {
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid", null);
        
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
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid", null);
        
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
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid", null);
        
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
        OpenAiCompatProvider provider = createProvider(localBaseUrl, "invalid", null);
        
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

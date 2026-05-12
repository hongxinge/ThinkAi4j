package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HTTP API 调用 Skill - 提供 HTTP 请求能力
 */
public class HttpSkill {

    private final OkHttpClient httpClient;
    private final Map<String, String> defaultHeaders = new ConcurrentHashMap<>();

    public HttpSkill() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public HttpSkill setDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
        return this;
    }

    @AiTool("发送HTTP GET请求")
    public String httpGet(
            @ToolParam(description = "请求URL") String url,
            @ToolParam(description = "请求头（JSON格式，可选）") String headers) {
        return executeRequest("GET", url, headers, null);
    }

    @AiTool("发送HTTP POST请求")
    public String httpPost(
            @ToolParam(description = "请求URL") String url,
            @ToolParam(description = "请求头（JSON格式，可选）") String headers,
            @ToolParam(description = "请求体（JSON格式）") String body) {
        return executeRequest("POST", url, headers, body);
    }

    @AiTool("发送HTTP PUT请求")
    public String httpPut(
            @ToolParam(description = "请求URL") String url,
            @ToolParam(description = "请求头（JSON格式，可选）") String headers,
            @ToolParam(description = "请求体（JSON格式）") String body) {
        return executeRequest("PUT", url, headers, body);
    }

    @AiTool("发送HTTP DELETE请求")
    public String httpDelete(
            @ToolParam(description = "请求URL") String url,
            @ToolParam(description = "请求头（JSON格式，可选）") String headers) {
        return executeRequest("DELETE", url, headers, null);
    }

    private String executeRequest(String method, String url, String headersJson, String body) {
        try {
            Request.Builder builder = new Request.Builder().url(url);
            defaultHeaders.forEach(builder::header);

            if (headersJson != null && !headersJson.isEmpty()) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, String> headers = mapper.readValue(headersJson, Map.class);
                headers.forEach(builder::header);
            }

            if ("GET".equals(method) || "DELETE".equals(method)) {
                builder.method(method, null);
            } else {
                MediaType jsonType = MediaType.parse("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(body != null ? body : "{}", jsonType);
                builder.method(method, requestBody);
            }

            try (Response response = httpClient.newCall(builder.build()).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                return "HTTP " + response.code() + "\n" + responseBody;
            }
        } catch (IOException e) {
            return "HTTP请求失败: " + e.getMessage();
        }
    }
}

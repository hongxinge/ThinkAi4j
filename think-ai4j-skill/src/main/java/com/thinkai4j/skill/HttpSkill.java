package com.thinkai4j.skill;

import com.thinkai4j.tool.annotation.AiTool;
import com.thinkai4j.tool.annotation.ToolParam;
import okhttp3.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HttpSkill {

    private final OkHttpClient httpClient;
    private final Map<String, String> defaultHeaders = new ConcurrentHashMap<>();
    private final Set<String> allowedHosts;
    private final boolean ssrfProtectionEnabled;
    private static final Set<String> BLOCKED_PRIVATE_RANGES = Set.of(
            "127.", "10.", "0.", "169.254.", "192.168."
    );
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost", "localhost.localdomain",
            "metadata.google.internal", "metadata.internal",
            "169.254.169.254"
    );

    public HttpSkill() {
        this(null, true);
    }

    public HttpSkill(Set<String> allowedHosts, boolean ssrfProtectionEnabled) {
        this.allowedHosts = allowedHosts;
        this.ssrfProtectionEnabled = ssrfProtectionEnabled;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public HttpSkill setDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
        return this;
    }

    public HttpSkill allowHost(String host) {
        if (allowedHosts != null) {
            allowedHosts.add(host);
        }
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
        if (url == null || url.trim().isEmpty()) {
            return "URL不能为空";
        }

        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            return "安全限制：仅允许HTTP/HTTPS协议";
        }

        if (ssrfProtectionEnabled) {
            String validationError = validateUrl(url);
            if (validationError != null) {
                return validationError;
            }
        }

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

    private String validateUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return "安全限制：URL中缺少主机地址";
            }

            String lowerHost = host.toLowerCase();
            for (String blocked : BLOCKED_HOSTNAMES) {
                if (lowerHost.equals(blocked) || lowerHost.endsWith("." + blocked)) {
                    return "安全限制：不允许访问内网地址 " + host;
                }
            }

            try {
                InetAddress[] addresses = InetAddress.getAllByName(host);
                for (InetAddress addr : addresses) {
                    String ip = addr.getHostAddress();
                    for (String blockedRange : BLOCKED_PRIVATE_RANGES) {
                        if (ip.startsWith(blockedRange)) {
                            return "安全限制：不允许访问内网地址 " + ip;
                        }
                    }
                    if (ip.startsWith("172.")) {
                        String[] parts = ip.split("\\.");
                        if (parts.length >= 2) {
                            int secondOctet = Integer.parseInt(parts[1]);
                            if (secondOctet >= 16 && secondOctet <= 31) {
                                return "安全限制：不允许访问内网地址 " + ip;
                            }
                        }
                    }
                }
            } catch (UnknownHostException e) {
                return "安全限制：无法解析主机地址 " + host;
            }

            if (allowedHosts != null && !allowedHosts.isEmpty() && !allowedHosts.contains(lowerHost)) {
                List<String> matches = new ArrayList<>();
                for (String allowed : allowedHosts) {
                    if (lowerHost.endsWith(allowed) || lowerHost.equals(allowed)) {
                        matches.add(allowed);
                    }
                }
                if (matches.isEmpty()) {
                    return "安全限制：主机 " + host + " 不在允许列表中";
                }
            }

            return null;
        } catch (Exception e) {
            return "安全限制：URL格式无效 - " + e.getMessage();
        }
    }
}

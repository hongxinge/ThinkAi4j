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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost", "localhost.localdomain",
            "metadata.google.internal", "metadata.internal"
    );

    public HttpSkill() {
        this(null, true);
    }

    public HttpSkill(Set<String> allowedHosts, boolean ssrfProtectionEnabled) {
        this.allowedHosts = allowedHosts != null ? ConcurrentHashMap.newKeySet() : null;
        if (allowedHosts != null) {
            this.allowedHosts.addAll(allowedHosts);
        }
        this.ssrfProtectionEnabled = ssrfProtectionEnabled;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                // 在 DNS 解析处校验实际连接的 IP，消除"校验与连接两次解析"之间的
                // DNS 重绑定（TOCTOU）绕过窗口
                .dns(this::resolveAndCheck)
                .build();
    }

    /**
     * DNS 解析时同步校验解析到的所有 IP，任一命中内网/保留地址即拒绝连接。
     */
    private List<InetAddress> resolveAndCheck(String hostname) throws UnknownHostException {
        List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
        if (ssrfProtectionEnabled) {
            for (InetAddress addr : addresses) {
                if (isBlockedAddress(addr)) {
                    throw new UnknownHostException(
                            "Blocked internal/reserved address: " + addr.getHostAddress());
                }
            }
        }
        return addresses;
    }

    /**
     * 判断地址是否为内网、回环、链路本地、保留或云元数据地址（同时覆盖 IPv4 与 IPv6）。
     */
    private static boolean isBlockedAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress()
                || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            // IPv4
            int b1 = bytes[0] & 0xFF, b2 = bytes[1] & 0xFF;
            // 10.0.0.0/8、172.16.0.0/12、192.168.0.0/16（isSiteLocalAddress 已覆盖，
            // 此处显式列出以保证语义清晰）、169.254.0.0/16、0.0.0.0/8、100.64.0.0/10（CGNAT）
            return (b1 == 10) || (b1 == 172 && b2 >= 16 && b2 <= 31)
                    || (b1 == 192 && b2 == 168) || (b1 == 169 && b2 == 254)
                    || (b1 == 0) || (b1 == 100 && b2 >= 64 && b2 <= 127);
        } else {
            // IPv6：IPv4-mapped（::ffff:a.b.c.d）按内嵌 IPv4 校验；
            // 唯一本地地址 fc00::/7；IPv6 链路本地 fe80::/10（isLinkLocalAddress 已覆盖）
            if (isIpv4Mapped(bytes)) {
                int b1 = bytes[12] & 0xFF, b2 = bytes[13] & 0xFF;
                return (b1 == 10) || (b1 == 127) || (b1 == 172 && b2 >= 16 && b2 <= 31)
                        || (b1 == 192 && b2 == 168) || (b1 == 169 && b2 == 254)
                        || (b1 == 0) || (b1 == 100 && b2 >= 64 && b2 <= 127);
            }
            return (bytes[0] & 0xFE) == 0xFC;
        }
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }

    public HttpSkill setDefaultHeader(String name, String value) {
        defaultHeaders.put(name, value);
        return this;
    }

    public HttpSkill allowHost(String host) {
        if (allowedHosts != null && host != null && !host.isBlank()) {
            allowedHosts.add(host.toLowerCase());
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
                Map<String, String> headers = objectMapper.readValue(headersJson, Map.class);
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

    /**
     * 请求前校验：主机名黑名单、DNS 解析结果、白名单。
     * IP 层校验同时在 OkHttp Dns 回调中执行，确保"校验的 IP"与"连接的 IP"一致。
     */
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
                    if (isBlockedAddress(addr)) {
                        return "安全限制：不允许访问内网地址 " + addr.getHostAddress();
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

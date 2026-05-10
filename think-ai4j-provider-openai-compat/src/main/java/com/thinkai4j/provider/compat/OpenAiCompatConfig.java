package com.thinkai4j.provider.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OpenAiCompatConfig {

    private List<ModelProvider> providers = new ArrayList<>();

    private HttpClientConfig httpClient = new HttpClientConfig();

    public List<ModelProvider> getProviders() {
        return providers;
    }

    public void setProviders(List<ModelProvider> providers) {
        this.providers = providers;
    }

    public HttpClientConfig getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(HttpClientConfig httpClient) {
        this.httpClient = httpClient;
    }

    public static class ModelProvider {

        private String name;
        private String baseUrl;
        private String apiKey;
        private String model;
        private Map<String, String> headers = new ConcurrentHashMap<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }
    }

    public static class HttpClientConfig {

        private ConnectionPool connectionPool = new ConnectionPool();
        private Timeout timeout = new Timeout();

        public ConnectionPool getConnectionPool() {
            return connectionPool;
        }

        public void setConnectionPool(ConnectionPool connectionPool) {
            this.connectionPool = connectionPool;
        }

        public Timeout getTimeout() {
            return timeout;
        }

        public void setTimeout(Timeout timeout) {
            this.timeout = timeout;
        }
    }

    public static class ConnectionPool {

        private int maxIdleConnections = 50;
        private long keepAliveMinutes = 5;

        public int getMaxIdleConnections() {
            return maxIdleConnections;
        }

        public void setMaxIdleConnections(int maxIdleConnections) {
            this.maxIdleConnections = maxIdleConnections;
        }

        public long getKeepAliveMinutes() {
            return keepAliveMinutes;
        }

        public void setKeepAliveMinutes(long keepAliveMinutes) {
            this.keepAliveMinutes = keepAliveMinutes;
        }
    }

    public static class Timeout {

        private long connectSeconds = 30;
        private long readSeconds = 60;
        private long writeSeconds = 30;

        public long getConnectSeconds() {
            return connectSeconds;
        }

        public void setConnectSeconds(long connectSeconds) {
            this.connectSeconds = connectSeconds;
        }

        public long getReadSeconds() {
            return readSeconds;
        }

        public void setReadSeconds(long readSeconds) {
            this.readSeconds = readSeconds;
        }

        public long getWriteSeconds() {
            return writeSeconds;
        }

        public void setWriteSeconds(long writeSeconds) {
            this.writeSeconds = writeSeconds;
        }
    }
}

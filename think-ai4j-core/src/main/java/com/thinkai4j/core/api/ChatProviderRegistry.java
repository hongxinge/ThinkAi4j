package com.thinkai4j.core.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.thinkai4j.core.exception.AiException;

public class ChatProviderRegistry {

    private String defaultProvider;
    private final Map<String, ChatProvider> providers = new ConcurrentHashMap<>();

    public void registerProvider(ChatProvider provider) {
        providers.put(provider.getProviderName(), provider);
    }

    public ChatProvider getProvider(String name) {
        return providers.get(name);
    }

    public ChatProvider getDefaultProviderInstance() {
        if (defaultProvider == null) {
            throw new AiException("No default provider configured. Please set think.ai.default-provider in application.yml");
        }
        ChatProvider provider = providers.get(defaultProvider);
        if (provider == null) {
            throw new AiException("Default provider '" + defaultProvider + "' not found. Available: " + getAvailableProviders());
        }
        return provider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public List<String> getAvailableProviders() {
        return providers.keySet().stream().toList();
    }

    public boolean hasProvider(String name) {
        return providers.containsKey(name);
    }
}

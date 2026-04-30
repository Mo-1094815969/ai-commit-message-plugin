package com.github.aicommit.provider;

public final class EffectiveProviderConfig {
    private final ProviderKind kind;
    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final String source;
    private final String wireApi;

    public EffectiveProviderConfig(ProviderKind kind, String apiKey, String baseUrl, String model, String source) {
        this(kind, apiKey, baseUrl, model, source, "");
    }

    public EffectiveProviderConfig(ProviderKind kind, String apiKey, String baseUrl, String model, String source,
                                   String wireApi) {
        this.kind = kind;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.source = source;
        this.wireApi = wireApi;
    }

    public ProviderKind getKind() {
        return kind;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String getSource() {
        return source;
    }

    public String getWireApi() {
        return wireApi;
    }
}

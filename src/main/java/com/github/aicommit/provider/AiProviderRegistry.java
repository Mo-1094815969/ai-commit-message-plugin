package com.github.aicommit.provider;

import java.util.EnumMap;
import java.util.Map;

public final class AiProviderRegistry {
    private final Map<ProviderKind, AiProvider> providers = new EnumMap<>(ProviderKind.class);

    public AiProviderRegistry() {
        register(new ClaudeProvider());
        register(new OpenAiProvider());
    }

    public AiProvider get(ProviderKind kind) {
        AiProvider provider = providers.get(kind);
        if (provider == null) {
            throw new IllegalStateException("Unsupported provider: " + kind);
        }
        return provider;
    }

    private void register(AiProvider provider) {
        providers.put(provider.kind(), provider);
    }
}

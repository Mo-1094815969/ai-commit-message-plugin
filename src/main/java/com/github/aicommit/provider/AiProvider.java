package com.github.aicommit.provider;

import java.io.IOException;

public interface AiProvider {
    ProviderKind kind();

    String generate(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException;
}

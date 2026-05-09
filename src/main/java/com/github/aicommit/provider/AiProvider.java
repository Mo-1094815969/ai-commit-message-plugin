package com.github.aicommit.provider;

import java.io.IOException;
import java.util.function.Consumer;

public interface AiProvider {
    ProviderKind kind();

    String generate(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException;

    default String generateStreaming(String prompt, EffectiveProviderConfig config, int timeoutSeconds,
                                     Consumer<String> chunkConsumer)
            throws IOException, InterruptedException {
        String result = generate(prompt, config, timeoutSeconds);
        if (chunkConsumer != null && !result.isEmpty()) {
            chunkConsumer.accept(result);
        }
        return result;
    }
}

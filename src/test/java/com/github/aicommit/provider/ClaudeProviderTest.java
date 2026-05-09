package com.github.aicommit.provider;

import org.junit.Assert;
import org.junit.Test;

public class ClaudeProviderTest {
    @Test
    public void extractsStreamingTextDeltaFromRawEventData() {
        String event = "{\"type\":\"content_block_delta\",\"delta\":{\"type\":\"text_delta\",\"text\":\"fix(provider): parse config\"}}";

        Assert.assertEquals("fix(provider): parse config", ClaudeProvider.extractStreamingText(event));
    }

    @Test
    public void extractsStreamingTextDeltaFromLegacyDataLine() {
        String event = "data: {\"delta\":{\"text\":\"- Restore toolbar icon\"}}";

        Assert.assertEquals("- Restore toolbar icon", ClaudeProvider.extractStreamingText(event));
    }
}

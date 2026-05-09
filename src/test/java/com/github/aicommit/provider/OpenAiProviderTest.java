package com.github.aicommit.provider;

import org.junit.Assert;
import org.junit.Test;

public class OpenAiProviderTest {
    @Test
    public void extractsStreamingChatDeltaFromRawEventData() {
        String event = "{\"choices\":[{\"delta\":{\"content\":\"fix(ui): restore icon\"}}]}";

        Assert.assertEquals("fix(ui): restore icon", OpenAiProvider.extractStreamingChatText(event));
    }

    @Test
    public void extractsStreamingChatDeltaFromLegacyDataLine() {
        String event = "data: {\"choices\":[{\"delta\":{\"content\":\"feat(skill): add rules\"}}]}";

        Assert.assertEquals("feat(skill): add rules", OpenAiProvider.extractStreamingChatText(event));
    }

    @Test
    public void extractsResponsesTextDelta() {
        String event = "{\"type\":\"response.output_text.delta\",\"delta\":\"- Add provider tests\"}";

        Assert.assertEquals("- Add provider tests", OpenAiProvider.extractStreamingResponsesText(event));
    }

    @Test
    public void ignoresDoneEvent() {
        Assert.assertEquals("", OpenAiProvider.extractStreamingChatText("[DONE]"));
    }
}

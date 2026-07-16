package com.github.aicommit.settings;

import org.junit.Assert;
import org.junit.Test;

public class AiCommitSettingsStateTest {
    @Test
    public void stateUsesCurrentDefaultModels() {
        AiCommitSettings.State state = new AiCommitSettings.State();

        Assert.assertEquals("claude-opus-4-8", state.claude.model);
        Assert.assertEquals("gpt-5.6-sol", state.openai.model);
    }

    @Test
    public void normalizeKeepsProviderConfigSavedFlag() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.providerConfigSaved = true;

        state.normalize();

        Assert.assertTrue(state.providerConfigSaved);
    }
}

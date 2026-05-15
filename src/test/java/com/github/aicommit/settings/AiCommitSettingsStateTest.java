package com.github.aicommit.settings;

import org.junit.Assert;
import org.junit.Test;

public class AiCommitSettingsStateTest {
    @Test
    public void normalizeKeepsProviderConfigSavedFlag() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.providerConfigSaved = true;

        state.normalize();

        Assert.assertTrue(state.providerConfigSaved);
    }
}

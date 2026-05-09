package com.github.aicommit.provider;

import com.github.aicommit.settings.AiCommitSettings;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProviderConfigResolverTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String originalUserHome = System.getProperty("user.home");

    @After
    public void restoreUserHome() {
        if (originalUserHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void manualConfigTakesPriorityOverLocalConfig() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        writeCodexConfig(home, "local-key", "https://local.example.com", "local-model", "responses");
        System.setProperty("user.home", home.toString());

        AiCommitSettings.State state = new AiCommitSettings.State();
        state.provider = AiCommitSettings.PROVIDER_OPENAI;
        state.openai.apiKey = "manual-key";
        state.openai.baseUrl = "https://manual.example.com";
        state.openai.model = "manual-model";

        EffectiveProviderConfig config = new ProviderConfigResolver().resolve(state);

        Assert.assertEquals(ProviderKind.OPENAI, config.getKind());
        Assert.assertEquals("manual-key", config.getApiKey());
        Assert.assertEquals("https://manual.example.com", config.getBaseUrl());
        Assert.assertEquals("manual-model", config.getModel());
        Assert.assertEquals("manual", config.getSource());
    }

    @Test
    public void resolvesCodexConfigFromLocalToml() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        writeCodexConfig(home, "local-key", "https://relay.example.com", "gpt-local", "responses");
        System.setProperty("user.home", home.toString());

        AiCommitSettings.State state = new AiCommitSettings.State();
        state.provider = AiCommitSettings.PROVIDER_OPENAI;
        EffectiveProviderConfig config = new ProviderConfigResolver().resolve(state);

        Assert.assertEquals(ProviderKind.OPENAI, config.getKind());
        Assert.assertEquals("local-key", config.getApiKey());
        Assert.assertEquals("https://relay.example.com", config.getBaseUrl());
        Assert.assertEquals("gpt-local", config.getModel());
        Assert.assertEquals("responses", config.getWireApi());
        Assert.assertEquals("local Codex config", config.getSource());
    }

    @Test
    public void resolvesClaudeConfigFromLocalSettings() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path claudeDir = home.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.write(claudeDir.resolve("settings.json"), (
                "{\n"
                        + "  \"env\": {\n"
                        + "    \"ANTHROPIC_API_KEY\": \"claude-key\",\n"
                        + "    \"ANTHROPIC_BASE_URL\": \"https://claude.example.com\",\n"
                        + "    \"ANTHROPIC_MODEL\": \"claude-local\"\n"
                        + "  }\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.home", home.toString());

        AiCommitSettings.State state = new AiCommitSettings.State();
        state.provider = AiCommitSettings.PROVIDER_CLAUDE;
        EffectiveProviderConfig config = new ProviderConfigResolver().resolve(state);

        Assert.assertEquals(ProviderKind.CLAUDE, config.getKind());
        Assert.assertEquals("claude-key", config.getApiKey());
        Assert.assertEquals("https://claude.example.com", config.getBaseUrl());
        Assert.assertEquals("claude-local", config.getModel());
        Assert.assertEquals("local Claude/Codex config", config.getSource());
    }

    @Test
    public void ignoresInvalidLocalJsonConfigFiles() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path claudeDir = home.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.write(claudeDir.resolve("settings.json"), "{invalid json".getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.home", home.toString());

        AiCommitSettings.State state = new AiCommitSettings.State();
        state.provider = AiCommitSettings.PROVIDER_OPENAI;
        state.openai.apiKey = "manual-key";

        EffectiveProviderConfig config = new ProviderConfigResolver().resolve(state);

        Assert.assertEquals("manual-key", config.getApiKey());
        Assert.assertEquals(ProviderKind.OPENAI, config.getKind());
    }

    private void writeCodexConfig(Path home, String apiKey, String baseUrl, String model, String wireApi)
            throws Exception {
        Path codexDir = home.resolve(".codex");
        Files.createDirectories(codexDir);
        Files.write(codexDir.resolve("config.toml"), (
                "model = \"" + model + "\"\n"
                        + "model_provider = \"relay\"\n\n"
                        + "[model_providers.relay]\n"
                        + "base_url = \"" + baseUrl + "\"\n"
                        + "env_key = \"OPENAI_API_KEY\"\n"
                        + "wire_api = \"" + wireApi + "\"\n\n"
                        + "[shell_environment_policy.set]\n"
                        + "OPENAI_API_KEY = \"" + apiKey + "\"\n").getBytes(StandardCharsets.UTF_8));
    }
}

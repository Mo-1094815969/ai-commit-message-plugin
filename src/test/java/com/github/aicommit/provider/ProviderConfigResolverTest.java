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
    public void exposesCodexLocalConfigForSettingsUi() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        writeCodexConfig(home, "local-key", "https://relay.example.com", "gpt-local", "responses");
        System.setProperty("user.home", home.toString());

        EffectiveProviderConfig config = new ProviderConfigResolver().resolveLocalToolConfig(ProviderKind.OPENAI);

        Assert.assertEquals(ProviderKind.OPENAI, config.getKind());
        Assert.assertEquals("local-key", config.getApiKey());
        Assert.assertEquals("https://relay.example.com", config.getBaseUrl());
        Assert.assertEquals("gpt-local", config.getModel());
        Assert.assertEquals("responses", config.getWireApi());
        Assert.assertEquals("local Codex config", config.getSource());
    }

    @Test
    public void resolvesCodexAuthTokenFromLocalEnv() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path codexDir = home.resolve(".codex");
        Files.createDirectories(codexDir);
        Files.write(codexDir.resolve("config.toml"), (
                "model_provider = \"relay\"\n"
                        + "model = \"gpt-5.5\"\n\n"
                        + "[model_providers.relay]\n"
                        + "base_url = \"https://relay.example.com/v1\"\n"
                        + "wire_api = \"responses\"\n\n"
                        + "[shell_environment_policy.set]\n"
                        + "OPENAI_AUTH_TOKEN = \"codex-token\"\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.home", home.toString());

        EffectiveProviderConfig config = new ProviderConfigResolver().resolveLocalToolConfig(ProviderKind.OPENAI);

        Assert.assertEquals("codex-token", config.getApiKey());
        Assert.assertEquals("https://relay.example.com/v1", config.getBaseUrl());
        Assert.assertEquals("gpt-5.5", config.getModel());
        Assert.assertEquals("responses", config.getWireApi());
    }

    @Test
    public void resolvesCodexApiKeyFromAuthJson() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path codexDir = home.resolve(".codex");
        Files.createDirectories(codexDir);
        Files.write(codexDir.resolve("config.toml"), (
                "model_provider = \"relay\"\n"
                        + "model = \"gpt-5.5\"\n\n"
                        + "[model_providers.relay]\n"
                        + "base_url = \"https://relay.example.com/v1\"\n"
                        + "wire_api = \"responses\"\n"
                        + "requires_openai_auth = true\n").getBytes(StandardCharsets.UTF_8));
        Files.write(codexDir.resolve("auth.json"), (
                "{\n"
                        + "  \"OPENAI_API_KEY\": \"auth-json-key\"\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.home", home.toString());

        EffectiveProviderConfig config = new ProviderConfigResolver().resolveLocalToolConfig(ProviderKind.OPENAI);

        Assert.assertEquals("auth-json-key", config.getApiKey());
        Assert.assertEquals("https://relay.example.com/v1", config.getBaseUrl());
        Assert.assertEquals("gpt-5.5", config.getModel());
        Assert.assertEquals("responses", config.getWireApi());
    }

    @Test
    public void resolvesCodexModelFromSelectedProfile() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path codexDir = home.resolve(".codex");
        Files.createDirectories(codexDir);
        Files.write(codexDir.resolve("config.toml"), (
                "profile = \"work\"\n"
                        + "model_provider = \"relay\"\n\n"
                        + "[profiles.work]\n"
                        + "model = \"gpt-5.3-codex\"\n\n"
                        + "[model_providers.relay]\n"
                        + "base_url = \"https://relay.example.com/v1\"\n\n"
                        + "[shell_environment_policy.set]\n"
                        + "CODEX_AUTH_TOKEN = \"codex-token\"\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.home", home.toString());

        EffectiveProviderConfig config = new ProviderConfigResolver().resolveLocalToolConfig(ProviderKind.OPENAI);

        Assert.assertEquals("codex-token", config.getApiKey());
        Assert.assertEquals("https://relay.example.com/v1", config.getBaseUrl());
        Assert.assertEquals("gpt-5.3-codex", config.getModel());
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
    public void exposesClaudeLocalConfigForSettingsUi() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        writeClaudeSettings(home, "claude-key", "https://claude.example.com", "claude-local");
        System.setProperty("user.home", home.toString());

        EffectiveProviderConfig config = new ProviderConfigResolver().resolveLocalToolConfig(ProviderKind.CLAUDE);

        Assert.assertEquals(ProviderKind.CLAUDE, config.getKind());
        Assert.assertEquals("claude-key", config.getApiKey());
        Assert.assertEquals("https://claude.example.com", config.getBaseUrl());
        Assert.assertEquals("claude-local", config.getModel());
        Assert.assertEquals("local Claude/Codex config", config.getSource());
    }

    @Test
    public void resolvesClaudeDefaultOpusModelFromLocalSettings() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path claudeDir = home.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.write(claudeDir.resolve("settings.json"), (
                "{\n"
                        + "  \"env\": {\n"
                        + "    \"ANTHROPIC_AUTH_TOKEN\": \"claude-key\",\n"
                        + "    \"ANTHROPIC_BASE_URL\": \"https://claude.example.com\",\n"
                        + "    \"ANTHROPIC_DEFAULT_OPUS_MODEL\": \"glm-5.1\"\n"
                        + "  }\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));
        System.setProperty("user.home", home.toString());

        EffectiveProviderConfig config = new ProviderConfigResolver().resolveLocalToolConfig(ProviderKind.CLAUDE);

        Assert.assertEquals("claude-key", config.getApiKey());
        Assert.assertEquals("https://claude.example.com", config.getBaseUrl());
        Assert.assertEquals("glm-5.1", config.getModel());
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

    private void writeClaudeSettings(Path home, String apiKey, String baseUrl, String model) throws Exception {
        Path claudeDir = home.resolve(".claude");
        Files.createDirectories(claudeDir);
        Files.write(claudeDir.resolve("settings.json"), (
                "{\n"
                        + "  \"env\": {\n"
                        + "    \"ANTHROPIC_API_KEY\": \"" + apiKey + "\",\n"
                        + "    \"ANTHROPIC_BASE_URL\": \"" + baseUrl + "\",\n"
                        + "    \"ANTHROPIC_MODEL\": \"" + model + "\"\n"
                        + "  }\n"
                        + "}\n").getBytes(StandardCharsets.UTF_8));
    }
}

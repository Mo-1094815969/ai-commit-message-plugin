package com.github.aicommit.provider;

import com.github.aicommit.settings.AiCommitSettings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProviderConfigResolver {
    private static final Logger LOG = Logger.getInstance(ProviderConfigResolver.class);

    public EffectiveProviderConfig resolveLocalToolConfig(ProviderKind kind) {
        if (kind == null) {
            return null;
        }
        return localToolConfig(kind, loadContext());
    }

    public EffectiveProviderConfig resolve(AiCommitSettings.State state) {
        ResolutionContext context = loadContext();
        ProviderKind selected = ProviderKind.fromId(state.provider);
        if (selected != null) {
            EffectiveProviderConfig config = resolveKind(selected, state, context);
            if (hasApiKey(config)) {
                return config;
            }
            throw new IllegalStateException("No API key found for provider: " + selected.id());
        }

        for (ProviderKind kind : supportedKinds()) {
            EffectiveProviderConfig manual = manualConfig(kind, state);
            if (hasApiKey(manual)) {
                return manual;
            }
        }
        for (ProviderKind kind : supportedKinds()) {
            EffectiveProviderConfig local = localToolConfig(kind, context);
            if (hasApiKey(local)) {
                return local;
            }
        }
        for (ProviderKind kind : supportedKinds()) {
            EffectiveProviderConfig env = envConfig(kind);
            if (hasApiKey(env)) {
                return env;
            }
        }
        throw new IllegalStateException("No AI provider is configured. Add a manual API key, local Claude/Codex config, or environment variable.");
    }

    private EffectiveProviderConfig resolveKind(ProviderKind kind, AiCommitSettings.State state,
                                                ResolutionContext context) {
        for (EffectiveProviderConfig config : Arrays.asList(
                manualConfig(kind, state),
                localToolConfig(kind, context),
                envConfig(kind))) {
            if (hasApiKey(config)) {
                return config;
            }
        }
        return null;
    }

    private ResolutionContext loadContext() {
        Path home = Paths.get(System.getProperty("user.home"));
        Path codexConfigPath = home.resolve(".codex").resolve("config.toml");
        return new ResolutionContext(
                readJsonFile(home.resolve(".claude").resolve("settings.json")),
                readJsonFile(home.resolve(".claude").resolve("settings.local.json")),
                readToml(codexConfigPath),
                readJsonFile(home.resolve(".codex").resolve("auth.json")));
    }

    private EffectiveProviderConfig manualConfig(ProviderKind kind, AiCommitSettings.State state) {
        AiCommitSettings.ProviderState providerState;
        if (kind == ProviderKind.CLAUDE) {
            providerState = state.claude;
        } else if (kind == ProviderKind.OPENAI) {
            providerState = state.openai;
        } else {
            return null;
        }
        return new EffectiveProviderConfig(kind,
                trim(providerState.apiKey),
                trim(providerState.baseUrl),
                trim(providerState.model),
                "manual",
                trim(providerState.wireApi));
    }

    private EffectiveProviderConfig localToolConfig(ProviderKind kind, ResolutionContext context) {
        if (kind == ProviderKind.CLAUDE) {
            return claudeLocalConfig(context);
        }
        if (kind == ProviderKind.OPENAI) {
            return codexLocalConfig(context);
        }
        return null;
    }

    private EffectiveProviderConfig envConfig(ProviderKind kind) {
        if (kind == ProviderKind.CLAUDE) {
            return new EffectiveProviderConfig(kind,
                    firstEnv("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "CLAUDE_API_KEY"),
                    firstEnv("ANTHROPIC_BASE_URL", "CLAUDE_BASE_URL"),
                    firstEnv("ANTHROPIC_MODEL", "ANTHROPIC_DEFAULT_OPUS_MODEL",
                            "ANTHROPIC_DEFAULT_SONNET_MODEL", "ANTHROPIC_DEFAULT_HAIKU_MODEL"),
                    "environment");
        }
        if (kind == ProviderKind.OPENAI) {
            return new EffectiveProviderConfig(kind,
                    firstEnv("OPENAI_API_KEY", "OPENAI_AUTH_TOKEN", "CODEX_API_KEY", "CODEX_AUTH_TOKEN"),
                    firstEnv("OPENAI_BASE_URL", "OPENAI_API_BASE"),
                    firstEnv("OPENAI_MODEL"),
                    "environment");
        }
        return null;
    }

    private EffectiveProviderConfig claudeLocalConfig(ResolutionContext context) {
        JsonObject env = new JsonObject();
        mergeJson(env, context.claudeSettings, "env");
        mergeJson(env, context.claudeLocalSettings, "env");
        mergeMap(env, context.codexConfig.sections.get("shell_environment_policy.set"));
        String apiKey = firstJson(env, new JsonObject(), "ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "CLAUDE_API_KEY");
        String baseUrl = firstJson(env, new JsonObject(), "ANTHROPIC_BASE_URL", "CLAUDE_BASE_URL");
        String model = firstJson(env, new JsonObject(), "ANTHROPIC_MODEL", "ANTHROPIC_DEFAULT_OPUS_MODEL",
                "ANTHROPIC_DEFAULT_SONNET_MODEL", "ANTHROPIC_DEFAULT_HAIKU_MODEL");
        return new EffectiveProviderConfig(ProviderKind.CLAUDE, apiKey, baseUrl, model, "local Claude/Codex config");
    }

    private EffectiveProviderConfig codexLocalConfig(ResolutionContext context) {
        TomlConfig toml = context.codexConfig;
        String providerName = toml.root.get("model_provider");
        Map<String, String> provider = providerName == null ? null : toml.sections.get("model_providers." + providerName);
        if ((provider == null || provider.isEmpty()) && !toml.sections.isEmpty()) {
            for (Map.Entry<String, Map<String, String>> entry : toml.sections.entrySet()) {
                if (entry.getKey().startsWith("model_providers.")) {
                    provider = entry.getValue();
                    break;
                }
            }
        }
        if (provider == null || provider.isEmpty()) {
            return null;
        }
        Map<String, String> profile = selectedCodexProfile(toml);
        Map<String, String> env = toml.sections.getOrDefault("shell_environment_policy.set", new LinkedHashMap<>());
        String envKey = firstValue(provider, "env_key", "api_key_env", "apiKeyEnv");
        String apiKey = firstValue(provider, "api_key", "apiKey", "OPENAI_API_KEY",
                "OPENAI_AUTH_TOKEN", "CODEX_API_KEY", "CODEX_AUTH_TOKEN");
        if (!notBlank(apiKey) && notBlank(envKey)) {
            apiKey = firstValue(env, envKey);
            if (!notBlank(apiKey)) {
                apiKey = System.getenv(envKey);
            }
        }
        if (!notBlank(apiKey)) {
            apiKey = firstValue(env, "OPENAI_API_KEY", "OPENAI_AUTH_TOKEN", "CODEX_API_KEY", "CODEX_AUTH_TOKEN");
        }
        if (!notBlank(apiKey)) {
            apiKey = firstJson(context.codexAuth, new JsonObject(), "OPENAI_API_KEY", "OPENAI_AUTH_TOKEN",
                    "CODEX_API_KEY", "CODEX_AUTH_TOKEN", "api_key", "auth_token");
        }
        if (!notBlank(apiKey)) {
            apiKey = firstEnv("OPENAI_API_KEY", "OPENAI_AUTH_TOKEN", "CODEX_API_KEY", "CODEX_AUTH_TOKEN");
        }
        String baseUrl = firstValue(provider, "base_url", "baseURL", "openai_base_url");
        String model = firstValue(toml.root, "model", "OPENAI_MODEL");
        if (!notBlank(model)) {
            model = firstValue(profile, "model", "OPENAI_MODEL");
        }
        if (!notBlank(model)) {
            model = firstValue(provider, "model");
        }
        String wireApi = firstValue(provider, "wire_api", "wireApi");
        if (!notBlank(apiKey) && isLocalUrl(baseUrl)) {
            apiKey = "local-codex";
        }
        return new EffectiveProviderConfig(ProviderKind.OPENAI, apiKey, baseUrl, model,
                "local Codex config", wireApi);
    }

    private Map<String, String> selectedCodexProfile(TomlConfig toml) {
        String profileName = firstValue(toml.root, "profile");
        if (notBlank(profileName)) {
            Map<String, String> selected = toml.sections.get("profiles." + profileName);
            if (selected != null) {
                return selected;
            }
        }
        Map<String, String> defaultProfile = toml.sections.get("profiles.default");
        return defaultProfile == null ? new LinkedHashMap<>() : defaultProfile;
    }

    private List<ProviderKind> supportedKinds() {
        return Arrays.asList(ProviderKind.CLAUDE, ProviderKind.OPENAI);
    }

    private JsonObject readJsonFile(Path path) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return parseObject(json);
        } catch (IOException e) {
            return null;
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse JSON config: " + path + ", " + e.getMessage());
            return null;
        }
    }

    private JsonObject parseObject(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new JsonObject();
        }
        try {
            JsonElement element = JsonParser.parseString(json);
            return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
        } catch (RuntimeException e) {
            LOG.debug("Failed to parse JSON object: " + e.getMessage());
            return new JsonObject();
        }
    }

    private String firstJson(JsonObject primary, JsonObject secondary, String... keys) {
        for (String key : keys) {
            String value = jsonString(primary, key);
            if (notBlank(value)) {
                return value;
            }
            value = jsonString(secondary, key);
            if (notBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private String jsonString(JsonObject object, String key) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private void mergeJson(JsonObject target, JsonObject source, String childObjectName) {
        if (target == null || source == null || !source.has(childObjectName)
                || !source.get(childObjectName).isJsonObject()) {
            return;
        }
        JsonObject child = source.getAsJsonObject(childObjectName);
        for (Map.Entry<String, JsonElement> entry : child.entrySet()) {
            if (!entry.getValue().isJsonNull()) {
                target.add(entry.getKey(), entry.getValue());
            }
        }
    }

    private void mergeMap(JsonObject target, Map<String, String> values) {
        if (target == null || values == null) {
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (notBlank(entry.getValue())) {
                target.addProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private TomlConfig readToml(Path path) {
        TomlConfig config = new TomlConfig();
        if (!Files.isRegularFile(path)) {
            return config;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            String section = "";
            for (String rawLine : lines) {
                String line = stripTomlComment(rawLine).trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    section = unquoteTomlPath(line.substring(1, line.length() - 1).trim());
                    config.sections.putIfAbsent(section, new LinkedHashMap<>());
                    continue;
                }
                int equals = line.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                String key = unquoteTomlValue(line.substring(0, equals).trim());
                String value = unquoteTomlValue(line.substring(equals + 1).trim());
                if (section.isEmpty()) {
                    config.root.put(key, value);
                } else {
                    config.sections.computeIfAbsent(section, ignored -> new LinkedHashMap<>()).put(key, value);
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to read TOML config: " + path + ", " + e.getMessage());
        }
        return config;
    }

    private String stripTomlComment(String line) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (c == '#' && !inSingle && !inDouble) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private String unquoteTomlPath(String value) {
        StringBuilder out = new StringBuilder();
        StringBuilder part = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                continue;
            }
            if (c == '.' && !inSingle && !inDouble) {
                appendTomlPathPart(out, part);
                continue;
            }
            part.append(c);
        }
        appendTomlPathPart(out, part);
        return out.toString();
    }

    private void appendTomlPathPart(StringBuilder out, StringBuilder part) {
        if (out.length() > 0) {
            out.append('.');
        }
        out.append(part.toString().trim());
        part.setLength(0);
    }

    private String unquoteTomlValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String firstValue(Map<String, String> values, String... keys) {
        if (values == null) {
            return "";
        }
        for (String key : keys) {
            String value = values.get(key);
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String firstEnv(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private boolean hasApiKey(EffectiveProviderConfig config) {
        return config != null && notBlank(config.getApiKey());
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isLocalUrl(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return normalized.startsWith("http://127.0.0.1")
                || normalized.startsWith("http://localhost")
                || normalized.startsWith("http://[::1]");
    }

    private static final class TomlConfig {
        private final Map<String, String> root = new LinkedHashMap<>();
        private final Map<String, Map<String, String>> sections = new LinkedHashMap<>();
    }

    private static final class ResolutionContext {
        private final JsonObject claudeSettings;
        private final JsonObject claudeLocalSettings;
        private final TomlConfig codexConfig;
        private final JsonObject codexAuth;

        private ResolutionContext(JsonObject claudeSettings, JsonObject claudeLocalSettings,
                                  TomlConfig codexConfig, JsonObject codexAuth) {
            this.claudeSettings = claudeSettings;
            this.claudeLocalSettings = claudeLocalSettings;
            this.codexConfig = codexConfig;
            this.codexAuth = codexAuth;
        }
    }
}

package com.github.aicommit.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service
@State(name = "AiCommitSettings", storages = @Storage("ai-commit-message.xml"))
public final class AiCommitSettings implements PersistentStateComponent<AiCommitSettings.State> {

    public static final String PROVIDER_AUTO = "auto";
    public static final String PROVIDER_CLAUDE = "claude";
    public static final String PROVIDER_OPENAI = "openai";
    public static final String UI_LANGUAGE_EN = "English";
    public static final String UI_LANGUAGE_ZH = "中文";
    public static final String DEFAULT_CLAUDE_MODEL = "claude-opus-4-7";
    public static final String DEFAULT_OPENAI_MODEL = "gpt-5.5";

    private State state = new State();

    public static AiCommitSettings getInstance() {
        return ApplicationManager.getApplication().getService(AiCommitSettings.class);
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
        this.state.normalize();
    }

    public State copyState() {
        State copy = new State();
        copy.uiLanguage = state.uiLanguage;
        copy.provider = state.provider;
        copy.language = state.language;
        copy.skillRef = state.skillRef;
        copy.timeoutSeconds = state.timeoutSeconds;
        copy.enableSensitiveFilter = state.enableSensitiveFilter;
        copy.excludePatterns = state.excludePatterns;
        copy.providerConfigSaved = state.providerConfigSaved;
        copy.claude.copyFrom(state.claude);
        copy.openai.copyFrom(state.openai);
        copy.claude.apiKey = SecretStore.getApiKey(PROVIDER_CLAUDE);
        copy.openai.apiKey = SecretStore.getApiKey(PROVIDER_OPENAI);
        copy.normalize();
        return copy;
    }

    public void update(State newState) {
        newState.normalize();
        newState.providerConfigSaved = true;
        SecretStore.setApiKey(PROVIDER_CLAUDE, newState.claude.apiKey);
        SecretStore.setApiKey(PROVIDER_OPENAI, newState.openai.apiKey);
        newState.claude.apiKey = "";
        newState.openai.apiKey = "";
        this.state = newState;
    }

    public static class State {
        public String uiLanguage = UI_LANGUAGE_EN;
        public String provider = PROVIDER_AUTO;
        public String language = "English";
        public String skillRef = "";
        public int timeoutSeconds = 75;
        public boolean enableSensitiveFilter = true;
        public boolean providerConfigSaved = false;
        public String excludePatterns = String.join("\n",
                ".env*",
                "*.pem",
                "*.key",
                "*.crt",
                "*.p12",
                "*.jks",
                "id_rsa*",
                "secrets.*",
                "*.lock",
                "package-lock.json",
                "pnpm-lock.yaml",
                "yarn.lock");
        public ProviderState claude = new ProviderState(DEFAULT_CLAUDE_MODEL);
        public ProviderState openai = new ProviderState(DEFAULT_OPENAI_MODEL);

        public void normalize() {
            if (!UI_LANGUAGE_ZH.equals(uiLanguage)) {
                uiLanguage = UI_LANGUAGE_EN;
            }
            if (provider == null || provider.trim().isEmpty()) {
                provider = PROVIDER_AUTO;
            }
            if (!PROVIDER_AUTO.equals(provider)
                    && !PROVIDER_CLAUDE.equals(provider)
                    && !PROVIDER_OPENAI.equals(provider)) {
                provider = PROVIDER_AUTO;
            }
            language = CommitLanguage.normalize(language);
            if (skillRef == null) {
                skillRef = "";
            }
            if (timeoutSeconds <= 0) {
                timeoutSeconds = 75;
            }
            timeoutSeconds = Math.min(Math.max(timeoutSeconds, 10), 120);
            if (excludePatterns == null || excludePatterns.trim().isEmpty()) {
                excludePatterns = ".env*\n*.pem\n*.key\n*.crt\n*.p12\n*.jks\nid_rsa*\nsecrets.*";
            }
            if (claude == null) {
                claude = new ProviderState(DEFAULT_CLAUDE_MODEL);
            }
            if (openai == null) {
                openai = new ProviderState(DEFAULT_OPENAI_MODEL);
            }
            claude.normalize();
            openai.normalize();
        }
    }

    public static class ProviderState {
        public transient String apiKey = "";
        public String baseUrl = "";
        public String model = "";
        public String wireApi = "";

        public ProviderState() {
        }

        public ProviderState(String model) {
            this.model = model;
        }

        public void copyFrom(ProviderState other) {
            this.apiKey = other.apiKey;
            this.baseUrl = other.baseUrl;
            this.model = other.model;
            this.wireApi = other.wireApi;
        }

        public void normalize() {
            if (apiKey == null) {
                apiKey = "";
            }
            if (baseUrl == null) {
                baseUrl = "";
            }
            if (model == null) {
                model = "";
            }
            if (wireApi == null) {
                wireApi = "";
            }
        }
    }
}

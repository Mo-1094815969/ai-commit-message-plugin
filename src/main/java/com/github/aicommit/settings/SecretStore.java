package com.github.aicommit.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;

public final class SecretStore {
    private static final String SERVICE_NAME = "AI Commit Message Generator";

    private SecretStore() {
    }

    public static String getApiKey(String provider) {
        Credentials credentials = PasswordSafe.getInstance().get(createAttributes(provider));
        if (credentials == null || credentials.getPassword() == null) {
            return "";
        }
        return credentials.getPasswordAsString();
    }

    public static void setApiKey(String provider, String apiKey) {
        String value = apiKey == null ? "" : apiKey.trim();
        PasswordSafe.getInstance().set(createAttributes(provider),
                value.isEmpty() ? null : new Credentials(provider, value));
    }

    private static CredentialAttributes createAttributes(String provider) {
        return new CredentialAttributes(SERVICE_NAME + ":" + provider, provider, SecretStore.class);
    }
}

package com.github.aicommit.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ClaudeProvider implements AiProvider {
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";

    @Override
    public ProviderKind kind() {
        return ProviderKind.CLAUDE;
    }

    @Override
    public String generate(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), "claude-sonnet-4-5"));
        body.addProperty("max_tokens", 1024);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        body.add("messages", messages);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", config.getApiKey());
        headers.put("anthropic-version", "2023-06-01");

        String url = trimTrailingSlash(valueOr(config.getBaseUrl(), DEFAULT_BASE_URL)) + "/v1/messages";
        JsonObject response = new JsonHttp().postJson(url, body, headers, timeoutSeconds);
        if (!response.has("content") || !response.get("content").isJsonArray()) {
            throw new IOException("Claude response did not contain content.");
        }
        JsonArray content = response.getAsJsonArray("content");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < content.size(); i++) {
            JsonObject item = content.get(i).getAsJsonObject();
            if (item.has("text")) {
                out.append(item.get("text").getAsString());
            }
        }
        return out.toString();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

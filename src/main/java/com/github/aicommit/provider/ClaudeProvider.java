package com.github.aicommit.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class ClaudeProvider implements AiProvider {
    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
    private static final int DEFAULT_MAX_TOKENS = 1536;

    @Override
    public ProviderKind kind() {
        return ProviderKind.CLAUDE;
    }

    @Override
    public String generate(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), "claude-sonnet-4-5"));
        body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        body.addProperty("temperature", 0.0);

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

    @Override
    public String generateStreaming(String prompt, EffectiveProviderConfig config, int timeoutSeconds,
                                    Consumer<String> chunkConsumer)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), "claude-sonnet-4-5"));
        body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        body.addProperty("temperature", 0.0);
        body.addProperty("stream", true);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add(message);
        body.add("messages", messages);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("x-api-key", config.getApiKey());
        headers.put("anthropic-version", "2023-06-01");

        StringBuilder out = new StringBuilder();
        String url = trimTrailingSlash(valueOr(config.getBaseUrl(), DEFAULT_BASE_URL)) + "/v1/messages";
        new JsonHttp().postJsonLines(url, body, headers, timeoutSeconds, line -> {
            String text = extractStreamingText(line);
            if (!text.isEmpty()) {
                out.append(text);
                if (chunkConsumer != null) {
                    chunkConsumer.accept(text);
                }
            }
        });
        return out.toString();
    }

    static String extractStreamingText(String eventData) {
        String data = sseData(eventData);
        if (data.isEmpty()) {
            return "";
        }
        try {
            JsonElement element = JsonParser.parseString(data);
            if (!element.isJsonObject()) {
                return "";
            }
            JsonObject event = element.getAsJsonObject();
            if (!event.has("delta") || !event.get("delta").isJsonObject()) {
                return "";
            }
            JsonObject delta = event.getAsJsonObject("delta");
            if (delta.has("text") && !delta.get("text").isJsonNull()) {
                return delta.get("text").getAsString();
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private static String sseData(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith("data:")) {
            return trimmed;
        }
        return trimmed.substring("data:".length()).trim();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}

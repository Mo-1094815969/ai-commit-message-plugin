package com.github.aicommit.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class OpenAiProvider implements AiProvider {
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";

    @Override
    public ProviderKind kind() {
        return ProviderKind.OPENAI;
    }

    @Override
    public String generate(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        if ("responses".equalsIgnoreCase(trim(config.getWireApi()))) {
            return generateWithResponsesApi(prompt, config, timeoutSeconds);
        }
        return generateWithChatCompletionsApi(prompt, config, timeoutSeconds);
    }

    private String generateWithChatCompletionsApi(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), "gpt-4.1-mini"));
        body.addProperty("temperature", 0.2);

        JsonArray messages = new JsonArray();
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);
        body.add("messages", messages);

        Map<String, String> headers = authHeaders(config.getApiKey());
        String url = endpointUrl(config.getBaseUrl(), "/chat/completions");
        JsonObject response = new JsonHttp().postJson(url, body, headers, timeoutSeconds);
        return extractChatCompletionText(response, "OpenAI");
    }

    private String generateWithResponsesApi(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), "gpt-4.1-mini"));
        body.addProperty("input", prompt);

        Map<String, String> headers = authHeaders(config.getApiKey());
        String url = endpointUrl(config.getBaseUrl(), "/responses");
        JsonObject response = new JsonHttp().postJson(url, body, headers, timeoutSeconds);
        return extractResponsesText(response, "OpenAI");
    }

    static String extractChatCompletionText(JsonObject response, String providerName) throws IOException {
        if (!response.has("choices") || !response.get("choices").isJsonArray()
                || response.getAsJsonArray("choices").size() == 0) {
            throw new IOException(providerName + " response did not contain choices.");
        }
        JsonObject firstChoice = response.getAsJsonArray("choices").get(0).getAsJsonObject();
        if (!firstChoice.has("message") || !firstChoice.get("message").isJsonObject()) {
            throw new IOException(providerName + " response did not contain message.");
        }
        JsonObject message = firstChoice.getAsJsonObject("message");
        if (!message.has("content") || message.get("content").isJsonNull()) {
            throw new IOException(providerName + " response message was empty.");
        }
        return message.get("content").getAsString();
    }

    static String extractResponsesText(JsonObject response, String providerName) throws IOException {
        if (response.has("output_text") && !response.get("output_text").isJsonNull()) {
            return response.get("output_text").getAsString();
        }
        if (!response.has("output") || !response.get("output").isJsonArray()) {
            throw new IOException(providerName + " response did not contain output.");
        }
        StringBuilder out = new StringBuilder();
        JsonArray output = response.getAsJsonArray("output");
        for (JsonElement outputElement : output) {
            if (!outputElement.isJsonObject()) {
                continue;
            }
            JsonObject outputItem = outputElement.getAsJsonObject();
            if (!outputItem.has("content") || !outputItem.get("content").isJsonArray()) {
                continue;
            }
            JsonArray content = outputItem.getAsJsonArray("content");
            for (JsonElement contentElement : content) {
                if (!contentElement.isJsonObject()) {
                    continue;
                }
                JsonObject contentItem = contentElement.getAsJsonObject();
                if (contentItem.has("text") && !contentItem.get("text").isJsonNull()) {
                    out.append(contentItem.get("text").getAsString());
                }
            }
        }
        if (out.length() == 0) {
            throw new IOException(providerName + " response output was empty.");
        }
        return out.toString();
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private Map<String, String> authHeaders(String apiKey) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            headers.put("Authorization", "Bearer " + apiKey.trim());
        }
        return headers;
    }

    private String endpointUrl(String baseUrl, String endpointPath) {
        String value = trimTrailingSlash(valueOr(baseUrl, DEFAULT_BASE_URL));
        if (value.endsWith("/chat/completions") || value.endsWith("/responses")) {
            return value;
        }
        if (value.endsWith("/v1")) {
            return value + endpointPath;
        }
        return value + "/v1" + endpointPath;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String out = value == null ? "" : value.trim();
        while (out.endsWith("/")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }
}

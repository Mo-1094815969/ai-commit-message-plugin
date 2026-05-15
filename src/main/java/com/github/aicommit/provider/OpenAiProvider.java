package com.github.aicommit.provider;

import com.github.aicommit.settings.AiCommitSettings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class OpenAiProvider implements AiProvider {
    private static final String DEFAULT_BASE_URL = "https://api.openai.com";
    private static final int DEFAULT_MAX_TOKENS = 1536;
    private static final String DIFF_SECTION_MARKER = "## Selected git diff";

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

    @Override
    public String generateStreaming(String prompt, EffectiveProviderConfig config, int timeoutSeconds,
                                    Consumer<String> chunkConsumer)
            throws IOException, InterruptedException {
        if ("responses".equalsIgnoreCase(trim(config.getWireApi()))) {
            return generateWithStreamingResponsesApi(prompt, config, timeoutSeconds, chunkConsumer);
        }
        return generateWithStreamingChatCompletionsApi(prompt, config, timeoutSeconds, chunkConsumer);
    }

    private String generateWithChatCompletionsApi(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), AiCommitSettings.DEFAULT_OPENAI_MODEL));
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);

        body.add("messages", buildChatMessages(prompt));

        Map<String, String> headers = authHeaders(config.getApiKey());
        String url = endpointUrl(config.getBaseUrl(), "/chat/completions");
        JsonObject response = new JsonHttp().postJson(url, body, headers, timeoutSeconds);
        return extractChatCompletionText(response, "OpenAI");
    }

    private String generateWithResponsesApi(String prompt, EffectiveProviderConfig config, int timeoutSeconds)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), AiCommitSettings.DEFAULT_OPENAI_MODEL));
        body.addProperty("max_output_tokens", DEFAULT_MAX_TOKENS);
        String[] parts = splitPrompt(prompt);
        if (!parts[0].isEmpty()) {
            body.addProperty("instructions", parts[0]);
        }
        body.addProperty("input", parts[1]);

        Map<String, String> headers = authHeaders(config.getApiKey());
        String url = endpointUrl(config.getBaseUrl(), "/responses");
        JsonObject response = new JsonHttp().postJson(url, body, headers, timeoutSeconds);
        return extractResponsesText(response, "OpenAI");
    }

    private String generateWithStreamingChatCompletionsApi(String prompt, EffectiveProviderConfig config,
                                                           int timeoutSeconds, Consumer<String> chunkConsumer)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), AiCommitSettings.DEFAULT_OPENAI_MODEL));
        body.addProperty("temperature", 0.3);
        body.addProperty("max_tokens", DEFAULT_MAX_TOKENS);
        body.addProperty("stream", true);
        body.add("messages", buildChatMessages(prompt));

        StringBuilder out = new StringBuilder();
        new JsonHttp().postJsonLines(endpointUrl(config.getBaseUrl(), "/chat/completions"), body,
                authHeaders(config.getApiKey()), timeoutSeconds, line -> {
                    String text = extractStreamingChatText(line);
                    if (!text.isEmpty()) {
                        out.append(text);
                        if (chunkConsumer != null) {
                            chunkConsumer.accept(text);
                        }
                    }
                });
        return out.toString();
    }

    private String generateWithStreamingResponsesApi(String prompt, EffectiveProviderConfig config, int timeoutSeconds,
                                                     Consumer<String> chunkConsumer)
            throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("model", valueOr(config.getModel(), AiCommitSettings.DEFAULT_OPENAI_MODEL));
        body.addProperty("max_output_tokens", DEFAULT_MAX_TOKENS);
        body.addProperty("stream", true);
        String[] parts = splitPrompt(prompt);
        if (!parts[0].isEmpty()) {
            body.addProperty("instructions", parts[0]);
        }
        body.addProperty("input", parts[1]);

        StringBuilder out = new StringBuilder();
        new JsonHttp().postJsonLines(endpointUrl(config.getBaseUrl(), "/responses"), body,
                authHeaders(config.getApiKey()), timeoutSeconds, line -> {
                    String text = extractStreamingResponsesText(line);
                    if (!text.isEmpty()) {
                        out.append(text);
                        if (chunkConsumer != null) {
                            chunkConsumer.accept(text);
                        }
                    }
                });
        return out.toString();
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

    static String extractStreamingChatText(String eventData) {
        String data = sseData(eventData);
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return "";
        }
        try {
            JsonElement element = JsonParser.parseString(data);
            if (!element.isJsonObject()) {
                return "";
            }
            JsonObject event = element.getAsJsonObject();
            if (!event.has("choices") || !event.get("choices").isJsonArray()
                    || event.getAsJsonArray("choices").size() == 0) {
                return "";
            }
            JsonObject choice = event.getAsJsonArray("choices").get(0).getAsJsonObject();
            if (!choice.has("delta") || !choice.get("delta").isJsonObject()) {
                return "";
            }
            JsonObject delta = choice.getAsJsonObject("delta");
            if (delta.has("content") && !delta.get("content").isJsonNull()) {
                return delta.get("content").getAsString();
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    static String extractStreamingResponsesText(String eventData) {
        String data = sseData(eventData);
        if (data.isEmpty() || "[DONE]".equals(data)) {
            return "";
        }
        try {
            JsonElement element = JsonParser.parseString(data);
            if (!element.isJsonObject()) {
                return "";
            }
            JsonObject event = element.getAsJsonObject();
            String type = event.has("type") && !event.get("type").isJsonNull()
                    ? event.get("type").getAsString()
                    : "";
            if (!"response.output_text.delta".equals(type)
                    && !"response.refusal.delta".equals(type)
                    && !"output_text.delta".equals(type)) {
                return "";
            }
            if (event.has("delta") && !event.get("delta").isJsonNull()) {
                return event.get("delta").getAsString();
            }
        } catch (Exception ignored) {
            return "";
        }
        return "";
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String[] splitPrompt(String prompt) {
        int idx = prompt.indexOf(DIFF_SECTION_MARKER);
        if (idx > 0) {
            return new String[]{prompt.substring(0, idx).trim(), prompt.substring(idx).trim()};
        }
        return new String[]{"", prompt};
    }

    private JsonArray buildChatMessages(String prompt) {
        JsonArray messages = new JsonArray();
        String[] parts = splitPrompt(prompt);
        if (!parts[0].isEmpty()) {
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", parts[0]);
            messages.add(system);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", parts[0].isEmpty() ? prompt : parts[1]);
        messages.add(user);
        return messages;
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
}

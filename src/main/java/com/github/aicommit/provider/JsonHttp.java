package com.github.aicommit.provider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class JsonHttp {
    private static final Gson GSON = new Gson();

    public JsonObject postJson(String url, JsonObject body, Map<String, String> headers, int timeoutSeconds)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30)))
                .build();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .header("Content-Type", "application/json");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getValue() != null && !header.getValue().trim().isEmpty()) {
                builder.header(header.getKey(), header.getValue());
            }
        }
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + ": " + shorten(response.body()));
        }
        return GSON.fromJson(response.body(), JsonObject.class);
    }

    public void postJsonLines(String url, JsonObject body, Map<String, String> headers, int timeoutSeconds,
                              Consumer<String> lineConsumer)
            throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(timeoutSeconds, 30)))
                .build();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            if (header.getValue() != null && !header.getValue().trim().isEmpty()) {
                builder.header(header.getKey(), header.getValue());
            }
        }
        HttpResponse<Stream<String>> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofLines());
        try (Stream<String> lines = response.body()) {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                StringBuilder error = new StringBuilder();
                lines.limit(20).forEach(line -> {
                    if (error.length() < 800) {
                        error.append(line).append('\n');
                    }
                });
                throw new IOException("HTTP " + response.statusCode() + ": " + shorten(error.toString()));
            }
            lines.forEach(lineConsumer);
        }
    }

    private String shorten(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 800 ? text.substring(0, 800) + "..." : text;
    }
}

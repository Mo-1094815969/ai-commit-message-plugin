package com.github.aicommit.provider;

public enum ProviderKind {
    CLAUDE("claude"),
    OPENAI("openai");

    private final String id;

    ProviderKind(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static ProviderKind fromId(String id) {
        if (id == null) {
            return null;
        }
        String normalized = id.trim().toLowerCase();
        for (ProviderKind kind : values()) {
            if (kind.id.equals(normalized)) {
                return kind;
            }
        }
        if ("gpt".equals(normalized) || "openai-compatible".equals(normalized)) {
            return OPENAI;
        }
        return null;
    }
}

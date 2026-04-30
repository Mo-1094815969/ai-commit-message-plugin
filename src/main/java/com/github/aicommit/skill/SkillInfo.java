package com.github.aicommit.skill;

public final class SkillInfo {
    private final String name;
    private final String description;
    private final String path;
    private final String providerId;

    public SkillInfo(String name, String description, String path, String providerId) {
        this.name = name;
        this.description = description;
        this.path = path;
        this.providerId = providerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPath() {
        return path;
    }

    public String getProviderId() {
        return providerId;
    }
}

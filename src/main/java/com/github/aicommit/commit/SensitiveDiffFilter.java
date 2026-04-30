package com.github.aicommit.commit;

import com.github.aicommit.settings.AiCommitSettings;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class SensitiveDiffFilter {
    private static final long MAX_FILE_CONTENT_CHARS = 16000;

    private final boolean enabled;
    private final List<PathMatcher> matchers = new ArrayList<>();

    public SensitiveDiffFilter(AiCommitSettings.State state) {
        this.enabled = state.enableSensitiveFilter;
        for (String line : state.excludePatterns.split("\\R")) {
            String pattern = line.trim();
            if (pattern.isEmpty() || pattern.startsWith("#")) {
                continue;
            }
            String normalized = pattern.replace('\\', '/');
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + normalized));
            if (!normalized.contains("/") && !normalized.contains("\\")) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/" + normalized));
            }
        }
    }

    public boolean shouldExclude(String path) {
        if (!enabled || path == null) {
            return false;
        }
        String normalized = path.replace('\\', '/');
        for (PathMatcher matcher : matchers) {
            if (matcher.matches(Paths.get(normalized))) {
                return true;
            }
        }
        return false;
    }

    public boolean isTooLarge(String content) {
        return content != null && content.length() > MAX_FILE_CONTENT_CHARS;
    }
}

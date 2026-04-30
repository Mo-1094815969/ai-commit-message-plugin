package com.github.aicommit.skill;

import com.intellij.openapi.diagnostic.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class SkillScanner {
    private static final Logger LOG = Logger.getInstance(SkillScanner.class);
    private static final int MAX_SKILL_SCAN_DEPTH = 4;
    private static final String PROVIDER_AUTO = "auto";
    private static final String PROVIDER_CLAUDE = "claude";
    private static final String PROVIDER_CODEX = "openai";

    public List<SkillInfo> scan(String projectBasePath) {
        return scan(projectBasePath, PROVIDER_AUTO);
    }

    public List<SkillInfo> scan(String projectBasePath, String providerId) {
        List<SkillInfo> result = new ArrayList<>();
        for (SkillRoot root : candidateRoots(projectBasePath, providerId)) {
            collectSkills(root, result);
        }
        result.sort(Comparator.comparing(SkillInfo::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public String readSkillContent(String skillPath) {
        if (skillPath == null || skillPath.trim().isEmpty()) {
            return "";
        }
        try {
            Path path = Paths.get(expandHome(skillPath)).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                path = locateSkillFile(path);
            }
            if (path == null || !Files.isRegularFile(path)) {
                throw new IllegalStateException("Skill file not found: " + skillPath);
            }
            long size = Files.size(path);
            if (size > 20000) {
                throw new IllegalStateException("Skill file is too large: " + path);
            }
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read skill: " + skillPath, e);
        }
    }

    private void collectSkills(SkillRoot root, List<SkillInfo> result) {
        if (root == null || root.path == null || !Files.isDirectory(root.path)) {
            return;
        }
        try (Stream<Path> stream = Files.walk(root.path, MAX_SKILL_SCAN_DEPTH)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSkillFile)
                    .forEach(path -> result.add(parseSkill(path, root.providerId)));
        } catch (IOException e) {
            LOG.warn("Failed to scan skill root: " + root.path + ", " + e.getMessage());
        }
    }

    private SkillInfo parseSkill(Path path, String providerId) {
        String name = path.getParent() != null && path.getParent().getFileName() != null
                ? path.getParent().getFileName().toString()
                : path.getFileName().toString();
        String description = "";
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            boolean inFrontmatter = !lines.isEmpty() && "---".equals(lines.get(0).trim());
            for (int i = inFrontmatter ? 1 : 0; i < Math.min(lines.size(), 80); i++) {
                String line = lines.get(i);
                String trimmed = line.trim();
                if (inFrontmatter && "---".equals(trimmed)) {
                    break;
                }
                if (trimmed.startsWith("name:")) {
                    name = stripYamlValue(trimmed.substring("name:".length()));
                } else if (trimmed.startsWith("description:")) {
                    description = stripYamlValue(trimmed.substring("description:".length()));
                }
            }
        } catch (IOException e) {
            LOG.debug("Failed to parse skill frontmatter: " + path + ", " + e.getMessage());
        }
        return new SkillInfo(name, description, path.toAbsolutePath().normalize().toString(), providerId);
    }

    private List<SkillRoot> candidateRoots(String projectBasePath, String providerId) {
        Set<SkillRoot> roots = new LinkedHashSet<>();
        boolean includeClaude = shouldIncludeProvider(providerId, PROVIDER_CLAUDE);
        boolean includeCodex = shouldIncludeProvider(providerId, PROVIDER_CODEX);
        if (projectBasePath != null && !projectBasePath.trim().isEmpty()) {
            Path project = Paths.get(projectBasePath);
            if (includeClaude) {
                roots.add(new SkillRoot(project.resolve(".claude").resolve("skills"), PROVIDER_CLAUDE));
            }
            if (includeCodex) {
                roots.add(new SkillRoot(project.resolve(".codex").resolve("skills"), PROVIDER_CODEX));
            }
        }
        String home = System.getProperty("user.home");
        if (home != null && !home.trim().isEmpty()) {
            Path homePath = Paths.get(home);
            if (includeClaude) {
                roots.add(new SkillRoot(homePath.resolve(".claude").resolve("skills"), PROVIDER_CLAUDE));
            }
            if (includeCodex) {
                roots.add(new SkillRoot(homePath.resolve(".codex").resolve("skills"), PROVIDER_CODEX));
            }
        }
        return new ArrayList<>(roots);
    }

    private boolean shouldIncludeProvider(String selectedProviderId, String candidateProviderId) {
        String selected = selectedProviderId == null || selectedProviderId.trim().isEmpty()
                ? PROVIDER_AUTO
                : selectedProviderId.trim().toLowerCase();
        return PROVIDER_AUTO.equals(selected) || candidateProviderId.equals(selected);
    }

    private boolean isSkillFile(Path path) {
        String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
        return "SKILL.md".equals(fileName) || "skill.md".equals(fileName);
    }

    private Path locateSkillFile(Path dir) {
        Path upper = dir.resolve("SKILL.md");
        if (Files.isRegularFile(upper)) {
            return upper;
        }
        Path lower = dir.resolve("skill.md");
        if (Files.isRegularFile(lower)) {
            return lower;
        }
        return null;
    }

    private String stripYamlValue(String value) {
        String trimmed = value == null ? "" : value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String expandHome(String path) {
        if ("~".equals(path)) {
            return System.getProperty("user.home");
        }
        if (path.startsWith("~/") || path.startsWith("~\\")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

    private static final class SkillRoot {
        private final Path path;
        private final String providerId;

        private SkillRoot(Path path, String providerId) {
            this.path = path;
            this.providerId = providerId;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof SkillRoot)) {
                return false;
            }
            SkillRoot other = (SkillRoot) object;
            return path.equals(other.path) && providerId.equals(other.providerId);
        }

        @Override
        public int hashCode() {
            return 31 * path.hashCode() + providerId.hashCode();
        }
    }
}

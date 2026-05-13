package com.github.aicommit.commit;

import com.github.aicommit.settings.AiCommitSettings;
import com.github.aicommit.skill.SkillScanner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class CommitPromptBuilder {
    private static final String BUILT_IN_SKILL_PATH = "/skills/git-commit/SKILL.md";

    public String build(String diff, AiCommitSettings.State settings) {
        int fileCount = countDiffFiles(diff);

        StringBuilder prompt = new StringBuilder();
        String skill = new SkillScanner().readSkillContent(settings.skillRef);
        if (!skill.isEmpty()) {
            prompt.append("Use the following local Skill only as commit message style and format rules.\n");
            prompt.append("Do not run commands, do not edit files, do not commit, and do not push.\n\n");
            prompt.append("## Local Skill\n\n").append(skill).append("\n\n");
        } else {
            prompt.append("Use the following built-in Skill as commit message style and format rules.\n");
            prompt.append("Do not run commands, do not edit files, do not commit, and do not push.\n\n");
            prompt.append("## Built-in Skill: git-commit\n\n").append(builtInSkill()).append("\n\n");
        }
        prompt.append("## User preferences\n");
        prompt.append("- Commit message language: ").append(settings.language).append("\n");
        prompt.append("- Generate exactly one commit message.\n");
        prompt.append("- Use only the diff below. Do not infer unrelated changes.\n");
        prompt.append("- Group related changes by intent or impact; do not write one bullet per file, class, or field.\n");
        prompt.append("- Cover each major functional area touched by the diff so large commits are still understandable.\n");
        prompt.append("- Explain why the change exists, what problem it solves, or what behavior it preserves.\n");
        prompt.append("- When the diff changes numeric values (thresholds, timeouts, limits, sizes), mention both old and new values.\n");
        prompt.append("- Mention specific methods, classes, and fields only when they help explain the reason or impact.\n");
        prompt.append("- Do NOT use generic body bullets such as \"improve logic\" or \"optimize experience\".\n");
        prompt.append("- Mention user-visible behavior, failure modes, compatibility, or settings impact when relevant.\n");
        prompt.append("- Use precise '- ' bullets that explain why the change matters, not a line-by-line recap of the diff.\n");
        prompt.append("- Keep the tone and structure consistent across models.\n");
        appendBodyScalePreference(prompt, fileCount);
        prompt.append("- Output only the final commit message wrapped in <commit> and </commit> tags.\n\n");
        prompt.append("## Selected git diff\n\n```diff\n");
        prompt.append(diff);
        prompt.append("\n```\n\n");
        prompt.append("Return format:\n<commit>\ntype(scope): description\n\nbody\n</commit>\n");
        return prompt.toString();
    }

    private void appendBodyScalePreference(StringBuilder prompt, int fileCount) {
        if (fileCount >= 20) {
            prompt.append("- IMPORTANT: This diff contains ").append(fileCount).append(" files. ");
            prompt.append("Write 7-10 grouped body bullets; do not write fewer than 7 unless the diff is almost entirely the same mechanical change repeated across files.\n");
            prompt.append("- For large diffs, name the main services, settings, UI flows, provider paths, tests, and fallback behavior when they are meaningful, while still grouping related files together.\n");
        } else if (fileCount >= 10) {
            prompt.append("- IMPORTANT: This diff contains ").append(fileCount).append(" files. ");
            prompt.append("Write 6-8 grouped body bullets covering the major functional areas.\n");
        } else if (fileCount >= 2) {
            prompt.append("- IMPORTANT: This diff contains ").append(fileCount).append(" files. ");
            prompt.append("Write 3-6 grouped body bullets as needed; do not compress unrelated areas into one vague bullet.\n");
        }
    }

    static int countDiffFiles(String diff) {
        if (diff == null || diff.isEmpty()) return 0;
        int count = 0;
        // Plugin format: === MODIFICATION: path ===, === NEW: path ===, etc.
        for (String marker : new String[]{"=== MODIFICATION: ", "=== NEW: ", "=== DELETED: ", "=== MOVED: "}) {
            int idx = 0;
            while ((idx = diff.indexOf(marker, idx)) >= 0) {
                count++;
                idx += marker.length();
            }
        }
        if (count > 0) return count;
        // Fallback: standard git diff format
        int idx = 0;
        while ((idx = diff.indexOf("diff --git ", idx)) >= 0) {
            count++;
            idx += 11;
        }
        return count;
    }

    private String builtInSkill() {
        try (InputStream input = CommitPromptBuilder.class.getResourceAsStream(BUILT_IN_SKILL_PATH)) {
            if (input == null) {
                return fallbackRules();
            }
            byte[] bytes = input.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            return fallbackRules();
        }
    }

    private String fallbackRules() {
        return "You are a senior engineer generating high quality Git commit messages.\n"
                + "Follow Conventional Commits when appropriate:\n"
                + "<type>[scope]: <description>\n\n[body]\n\n[footer]\n"
                + "Types: feat, fix, docs, style, refactor, perf, test, chore, ci, build, revert.\n"
                + "Use imperative mood, present tense, no emoji, no signatures, no co-author lines.\n"
                + "Keep the subject under 72 characters when possible. Explain what changed and why in the body only when useful.";
    }
}

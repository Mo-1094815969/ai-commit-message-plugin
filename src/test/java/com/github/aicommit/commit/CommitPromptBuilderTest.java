package com.github.aicommit.commit;

import com.github.aicommit.settings.AiCommitSettings;
import org.junit.Assert;
import org.junit.Test;

public class CommitPromptBuilderTest {

    @Test
    public void encouragesBodyBulletsForComplexDiffs() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.language = "English";

        String prompt = new CommitPromptBuilder().build(
                "=== MODIFICATION: A.java ===\n+ line\n" +
                        "=== NEW: B.java ===\n+ line\n",
                state);

        Assert.assertTrue(prompt.contains("MUST write at least 2 body bullets"));
        Assert.assertTrue(prompt.contains("Do NOT collapse changes from different files into one bullet"));
        Assert.assertTrue(prompt.contains("Use precise '- ' bullets"));
    }

    @Test
    public void injectsFileCountForLargeDiffs() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.language = "English";

        StringBuilder diff = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            diff.append("=== MODIFICATION: File").append(i).append(".java ===\n+ line\n");
        }

        String prompt = new CommitPromptBuilder().build(diff.toString(), state);

        Assert.assertTrue(prompt.contains("This diff contains 10 files"));
        Assert.assertTrue(prompt.contains("MUST write at least 7 body bullets"));
    }

    @Test
    public void singleFileOmitsMinBulletRequirement() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.language = "English";

        String prompt = new CommitPromptBuilder().build(
                "=== MODIFICATION: A.java ===\n+ line\n",
                state);

        Assert.assertFalse(prompt.contains("MUST write at least"));
    }

    @Test
    public void countDiffFiles() {
        Assert.assertEquals(0, CommitPromptBuilder.countDiffFiles("no diff here"));
        Assert.assertEquals(1, CommitPromptBuilder.countDiffFiles("=== MODIFICATION: A.java ===\n+ line\n"));
        Assert.assertEquals(2, CommitPromptBuilder.countDiffFiles(
                "=== MODIFICATION: A.java ===\n+ line\n=== NEW: B.java ===\n+ line\n"));
        Assert.assertEquals(3, CommitPromptBuilder.countDiffFiles(
                "=== MODIFICATION: A.java ===\n=== DELETED: B.java ===\n=== NEW: C.java ===\n"));
        // fallback: standard git diff format
        Assert.assertEquals(2, CommitPromptBuilder.countDiffFiles(
                "diff --git a/A.java b/A.java\ndiff --git a/B.java b/B.java\n"));
    }

    @Test
    public void returnFormatRequiresBody() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        String prompt = new CommitPromptBuilder().build("diff --git a/A.java b/A.java\n", state);

        Assert.assertTrue(prompt.contains("<commit>\ntype(scope): description\n\nbody\n</commit>"));
        Assert.assertFalse(prompt.contains("optional body"));
    }
}

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

        Assert.assertTrue(prompt.contains("Write 3-6 grouped body bullets"));
        Assert.assertTrue(prompt.contains("do not write one bullet per file, class, or field"));
        Assert.assertTrue(prompt.contains("why the change matters, not a line-by-line recap of the diff"));
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
        Assert.assertTrue(prompt.contains("Write 6-8 grouped body bullets"));
        Assert.assertTrue(prompt.contains("covering the major functional areas"));
    }

    @Test
    public void requiresEnoughBodyBulletsForVeryLargeDiffs() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.language = "English";

        StringBuilder diff = new StringBuilder();
        for (int i = 0; i < 38; i++) {
            diff.append("=== MODIFICATION: File").append(i).append(".java ===\n+ line\n");
        }

        String prompt = new CommitPromptBuilder().build(diff.toString(), state);

        Assert.assertTrue(prompt.contains("This diff contains 38 files"));
        Assert.assertTrue(prompt.contains("Write 7-10 grouped body bullets"));
        Assert.assertTrue(prompt.contains("do not write fewer than 7"));
        Assert.assertTrue(prompt.contains("name the main services, settings, UI flows"));
    }

    @Test
    public void singleFileOmitsMinBulletRequirement() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.language = "English";

        String prompt = new CommitPromptBuilder().build(
                "=== MODIFICATION: A.java ===\n+ line\n",
                state);

        Assert.assertFalse(prompt.contains("MUST write at least"));
        Assert.assertTrue(prompt.contains("This diff changes one file"));
        Assert.assertTrue(prompt.contains("avoid generic filler bullets"));
    }

    @Test
    public void complexSingleFileRequiresConcreteBodyBullets() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.language = "English";

        StringBuilder diff = new StringBuilder("=== MODIFICATION: AiCommitConfigurable.java ===\n");
        for (int i = 0; i < 18; i++) {
            diff.append("- old line ").append(i).append("\n");
            diff.append("+ new line ").append(i).append("\n");
        }

        String prompt = new CommitPromptBuilder().build(diff.toString(), state);

        Assert.assertTrue(prompt.contains("one file but is non-trivial (36 changed lines)"));
        Assert.assertTrue(prompt.contains("Write 3-5 concrete body bullets"));
        Assert.assertTrue(prompt.contains("Do not collapse a complex single-file diff into one generic bullet"));
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
    public void countChangedLinesIgnoresDiffHeaders() {
        Assert.assertEquals(0, CommitPromptBuilder.countChangedLines(null));
        Assert.assertEquals(2, CommitPromptBuilder.countChangedLines(
                "+++ b/A.java\n--- a/A.java\n+ new\n- old\n context\n"));
    }

    @Test
    public void returnFormatRequiresBody() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        String prompt = new CommitPromptBuilder().build("diff --git a/A.java b/A.java\n", state);

        Assert.assertTrue(prompt.contains("<commit>\ntype(scope): description\n\nbody\n</commit>"));
        Assert.assertFalse(prompt.contains("optional body"));
    }
}

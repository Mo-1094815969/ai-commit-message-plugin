package com.github.aicommit.commit;

import org.junit.Assert;
import org.junit.Test;

public class CommitMessageCleanerTest {

    @Test
    public void extractsCommitTagContent() {
        String raw = "analysis\n<commit>\nfeat(commit): Add AI generator\n\nUse selected changes.\n</commit>\n";
        String cleaned = new CommitMessageCleaner().clean(raw);
        Assert.assertEquals("feat(commit): Add AI generator\n\nUse selected changes.", cleaned);
    }

    @Test
    public void convertsLiteralNewlines() {
        String raw = "<commit>fix: Handle timeout\\n\\nWrite error to input</commit>";
        String cleaned = new CommitMessageCleaner().clean(raw);
        Assert.assertEquals("fix: Handle timeout\n\nWrite error to input", cleaned);
    }

    @Test
    public void extractsPartialCommitTagContent() {
        String raw = "analysis\n<commit>\nfeat(skill): Add built-in";
        String cleaned = new CommitMessageCleaner().cleanPartial(raw);
        Assert.assertEquals("feat(skill): Add built-in", cleaned);
    }

    @Test
    public void convertsPartialLiteralNewlines() {
        String raw = "<commit>fix(ui): Restore icon\\n\\n- Reset icon after success";
        String cleaned = new CommitMessageCleaner().cleanPartial(raw);
        Assert.assertEquals("fix(ui): Restore icon\n\n- Reset icon after success", cleaned);
    }

    @Test
    public void hidesContentBeforeCommitTagInPartialMode() {
        String raw = "analysis before tag\n<commit>feat(ui): Restore icon";
        String cleaned = new CommitMessageCleaner().cleanPartial(raw);
        Assert.assertEquals("feat(ui): Restore icon", cleaned);
    }

    @Test
    public void trimsPartialEndTagFragments() {
        String raw = "<commit>fix(ui): Restore icon\n\n- Reset icon after success\n</comm";
        String cleaned = new CommitMessageCleaner().cleanPartial(raw);
        Assert.assertEquals("fix(ui): Restore icon\n\n- Reset icon after success", cleaned);
    }
}

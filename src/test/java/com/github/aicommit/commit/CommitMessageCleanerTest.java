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
}

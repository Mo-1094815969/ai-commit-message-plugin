package com.github.aicommit.commit;

import org.junit.Assert;
import org.junit.Test;

public class GitDiffCollectorTest {
    @Test
    public void lineDiffDoesNotMarkUnchangedTrailingLinesAfterInsertion() {
        String before = "alpha\ncharlie\n";
        String after = "alpha\nbravo\ncharlie\n";

        String diff = GitDiffCollector.lineDiff(before, after);

        Assert.assertEquals("+ bravo\n", diff);
    }

    @Test
    public void lineDiffKeepsIndependentAdditionsAndDeletionsPrecise() {
        String before = "alpha\nold\ncharlie\nremove-me\n";
        String after = "alpha\nnew\ncharlie\n";

        String diff = GitDiffCollector.lineDiff(before, after);

        Assert.assertEquals("- old\n+ new\n- remove-me\n", diff);
    }

    @Test
    public void lineDiffKeepsLargeFileChangesVisible() {
        String before = repeatedLines("same", 900) + "old behavior\n";
        String after = repeatedLines("same", 900) + "new behavior\n";

        String diff = GitDiffCollector.lineDiff(before, after);

        Assert.assertTrue(diff.contains("- old behavior"));
        Assert.assertTrue(diff.contains("+ new behavior"));
        Assert.assertFalse(diff.contains("large file content omitted"));
    }

    @Test
    public void lineDiffTruncatesVeryLongChangedLines() {
        String before = "old " + repeatedText("a", 800) + "\n";
        String after = "new " + repeatedText("b", 800) + "\n";

        String diff = GitDiffCollector.lineDiff(before, after);

        Assert.assertTrue(diff.contains("[line truncated]"));
        Assert.assertTrue(diff.length() < 1300);
    }

    private static String repeatedLines(String line, int count) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < count; i++) {
            text.append(line).append("\n");
        }
        return text.toString();
    }

    private static String repeatedText(String text, int count) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < count; i++) {
            out.append(text);
        }
        return out.toString();
    }
}

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
}

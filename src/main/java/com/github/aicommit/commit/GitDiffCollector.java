package com.github.aicommit.commit;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangesUtil;
import com.intellij.openapi.vcs.changes.ContentRevision;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class GitDiffCollector {
    private static final Logger LOG = Logger.getInstance(GitDiffCollector.class);
    private static final int MAX_TOTAL_DIFF_LENGTH = 12000;
    private static final int MAX_NEW_FILE_CHARS = 1200;
    private static final int MAX_CHANGED_LINES = 80;

    public String collect(@NotNull Collection<Change> changes, @NotNull SensitiveDiffFilter filter) {
        StringBuilder diff = new StringBuilder();
        int excluded = 0;
        for (Change change : changes) {
            try {
                FilePath filePath = ChangesUtil.getFilePath(change);
                String path = filePath.getPath();
                if (filter.shouldExclude(path)) {
                    excluded++;
                    continue;
                }
                appendChange(diff, change, path, filter);
                if (diff.length() > MAX_TOTAL_DIFF_LENGTH) {
                    diff.append("\n... diff truncated because it is too long\n");
                    break;
                }
            } catch (Exception e) {
                LOG.warn("Failed to collect diff for change: " + e.getMessage());
            }
        }
        if (excluded > 0) {
            diff.append("\nFiltered sensitive files: ").append(excluded).append("\n");
        }
        return diff.toString().trim();
    }

    private void appendChange(StringBuilder diff, Change change, String path, SensitiveDiffFilter filter)
            throws VcsException {
        diff.append("\n=== ").append(change.getType().name()).append(": ").append(path).append(" ===\n");
        ContentRevision beforeRevision = change.getBeforeRevision();
        ContentRevision afterRevision = change.getAfterRevision();

        if (change.getType() == Change.Type.NEW && afterRevision != null) {
            String content = afterRevision.getContent();
            if (content == null) {
                diff.append("+++ [new binary or unavailable file]\n");
            } else if (filter.isTooLarge(content)) {
                diff.append("+++ [new file is too large and was omitted]\n");
            } else {
                int end = Math.min(content.length(), MAX_NEW_FILE_CHARS);
                diff.append("+++ ").append(content, 0, end).append("\n");
                if (content.length() > end) {
                    diff.append("... new file truncated\n");
                }
            }
            return;
        }

        if (change.getType() == Change.Type.DELETED) {
            diff.append("--- file deleted\n");
            return;
        }

        if (beforeRevision == null || afterRevision == null) {
            diff.append("[content unavailable]\n");
            return;
        }
        String before = beforeRevision.getContent();
        String after = afterRevision.getContent();
        if (before == null || after == null) {
            diff.append("[binary or unavailable content]\n");
            return;
        }
        if (filter.isTooLarge(before) || filter.isTooLarge(after)) {
            diff.append("[large file content omitted]\n");
            return;
        }
        diff.append(simpleLineDiff(before, after));
    }

    private String simpleLineDiff(String before, String after) {
        String[] beforeLines = before.split("\\R", -1);
        String[] afterLines = after.split("\\R", -1);
        int max = Math.max(beforeLines.length, afterLines.length);
        int shown = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < max && shown < MAX_CHANGED_LINES; i++) {
            String left = i < beforeLines.length ? beforeLines[i] : "";
            String right = i < afterLines.length ? afterLines[i] : "";
            if (!left.equals(right)) {
                if (!left.isEmpty()) {
                    out.append("- ").append(left).append("\n");
                    shown++;
                }
                if (!right.isEmpty() && shown < MAX_CHANGED_LINES) {
                    out.append("+ ").append(right).append("\n");
                    shown++;
                }
            }
        }
        if (shown >= MAX_CHANGED_LINES) {
            out.append("... changed lines truncated\n");
        }
        return out.toString();
    }
}

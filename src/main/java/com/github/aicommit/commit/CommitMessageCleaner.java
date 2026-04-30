package com.github.aicommit.commit;

public final class CommitMessageCleaner {
    private static final String START = "<commit>";
    private static final String END = "</commit>";

    public String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        int start = value.indexOf(START);
        int end = value.indexOf(END);
        if (start >= 0 && end > start) {
            value = value.substring(start + START.length(), end).trim();
        }
        if (value.startsWith("```")) {
            int firstNewline = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                value = value.substring(firstNewline + 1, lastFence).trim();
            }
        }
        value = value.replace("\\n", "\n");
        value = value.replaceAll("(?i)generated with .*", "").trim();
        return value.replaceAll("\\n{3,}", "\n\n").trim();
    }
}

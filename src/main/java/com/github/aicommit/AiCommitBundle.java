package com.github.aicommit;

import java.text.MessageFormat;
import java.util.ResourceBundle;

public final class AiCommitBundle {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("messages.AiCommitBundle");

    private AiCommitBundle() {
    }

    public static String message(String key, Object... params) {
        String value = BUNDLE.getString(key);
        return params == null || params.length == 0 ? value : MessageFormat.format(value, params);
    }
}

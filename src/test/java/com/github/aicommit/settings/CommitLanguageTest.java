package com.github.aicommit.settings;

import org.junit.Assert;
import org.junit.Test;

public class CommitLanguageTest {
    @Test
    public void returnsSimplifiedChineseDisplayName() {
        Assert.assertEquals("简体中文", CommitLanguage.displayName(CommitLanguage.SIMPLIFIED_CHINESE));
    }

    @Test
    public void mapsDisplayNameBackToStableValue() {
        Assert.assertEquals(CommitLanguage.SIMPLIFIED_CHINESE, CommitLanguage.valueFromDisplayName("简体中文"));
    }

    @Test
    public void returnsGeneratingMessageForSimplifiedChinese() {
        Assert.assertEquals("正在生成提交信息...",
                CommitLanguage.generatingMessage(CommitLanguage.SIMPLIFIED_CHINESE));
    }

    @Test
    public void normalizesLocalizedDisplayName() {
        Assert.assertEquals(CommitLanguage.SIMPLIFIED_CHINESE, CommitLanguage.normalize("简体中文"));
    }

    @Test
    public void returnsFailureMessageForSimplifiedChinese() {
        Assert.assertEquals("生成提交信息失败：network error",
                CommitLanguage.failureMessage(CommitLanguage.SIMPLIFIED_CHINESE, "network error"));
    }

    @Test
    public void returnsTimeoutMessageForSimplifiedChinese() {
        Assert.assertEquals("生成提交信息超时：75 秒。",
                CommitLanguage.timeoutMessage(CommitLanguage.SIMPLIFIED_CHINESE, 75));
    }

    @Test
    public void returnsNoChangesMessageForSimplifiedChinese() {
        Assert.assertEquals("未选择变更文件。请先在变更列表中选择要生成提交信息的文件。",
                CommitLanguage.noChangesMessage(CommitLanguage.SIMPLIFIED_CHINESE));
    }

    @Test
    public void returnsAlreadyRunningMessageForSimplifiedChinese() {
        Assert.assertEquals("提交信息正在生成中。",
                CommitLanguage.alreadyRunningMessage(CommitLanguage.SIMPLIFIED_CHINESE));
    }
}

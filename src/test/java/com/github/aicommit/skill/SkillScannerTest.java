package com.github.aicommit.skill;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SkillScannerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final String originalUserHome = System.getProperty("user.home");

    @After
    public void restoreUserHome() {
        if (originalUserHome == null) {
            System.clearProperty("user.home");
        } else {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void usesCacheUntilForceRefresh() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path project = temporaryFolder.newFolder("project").toPath();
        System.setProperty("user.home", home.toString());

        writeSkill(project.resolve(".codex").resolve("skills").resolve("one"), "one");
        SkillScanner scanner = new SkillScanner();

        Assert.assertEquals(1, scanner.scan(project.toString(), "openai").size());

        writeSkill(project.resolve(".codex").resolve("skills").resolve("two"), "two");
        Assert.assertEquals(1, scanner.scan(project.toString(), "openai").size());
        Assert.assertEquals(2, scanner.scan(project.toString(), "openai", true).size());
    }

    @Test
    public void filtersSkillsBySelectedProvider() throws Exception {
        Path home = temporaryFolder.newFolder("home").toPath();
        Path project = temporaryFolder.newFolder("project").toPath();
        System.setProperty("user.home", home.toString());

        writeSkill(project.resolve(".claude").resolve("skills").resolve("claude-only"), "claude-only");
        writeSkill(project.resolve(".codex").resolve("skills").resolve("codex-only"), "codex-only");

        SkillScanner scanner = new SkillScanner();
        List<SkillInfo> claude = scanner.scan(project.toString(), "claude", true);
        List<SkillInfo> codex = scanner.scan(project.toString(), "openai", true);

        Assert.assertEquals(1, claude.size());
        Assert.assertEquals("claude", claude.get(0).getProviderId());
        Assert.assertEquals(1, codex.size());
        Assert.assertEquals("openai", codex.get(0).getProviderId());
    }

    private void writeSkill(Path dir, String name) throws Exception {
        Files.createDirectories(dir);
        Files.write(dir.resolve("SKILL.md"), (
                "---\n"
                        + "name: " + name + "\n"
                        + "description: test skill\n"
                        + "---\n"
                        + "Use this skill.\n").getBytes(StandardCharsets.UTF_8));
    }
}

# AI Commit Message Generator

[English](README.md) | [简体中文](README.zh-CN.md)

Standalone JetBrains IDE plugin MVP for generating Git commit messages from the files currently selected in the Commit tool window.

## MVP Scope

- Adds an AI generate action to the Commit message toolbar.
- Uses only the files selected for commit.
- Writes the generated message directly into the Commit Message input.
- Shows a loading icon while generation is running.
- Times out after 60 seconds by default.
- Writes an error message into the Commit Message input when generation fails.
- Supports Claude and Codex/OpenAI-compatible relays.
- Provides an English/Chinese settings UI switch.
- Supports OpenAI-compatible and Claude-compatible relay services through manual Base URL settings.
- Resolves credentials with this priority:
  - manual settings
  - local Claude/Codex config
  - read-only cc-switch config
  - environment variables
- Uses a built-in `git-commit` Skill as the default commit-message style guide.
- Scans provider-specific local Skills and lets the user override the built-in default.
- Filters sensitive paths before sending diff content to AI.

## Settings

Open `Settings | Tools | AI Commit Message`.

Manual API keys are stored in JetBrains PasswordSafe. Base URL, model, language, timeout, Skill, and exclude patterns are stored in the plugin settings file.

When no local Skill is selected, the plugin uses the bundled `git-commit` Skill. It is tailored for this plugin: it generates one Conventional Commits message from the selected diff only, respects the configured output language, and never asks the AI to run commands, commit, or push.

For Codex/OpenAI-compatible relays, Base URL accepts the relay root URL, `/v1`, `/v1/chat/completions`, or `/v1/responses`. Use the Wire API option to choose Chat Completions or Responses.

When manual fields are empty, the plugin can read local tool configs:

- Claude: `~/.claude/settings.json`, `~/.claude/settings.local.json`
- Codex: `~/.codex/config.toml`, including `model_provider`, `model`, `base_url`, and `wire_api`

## Environment Variables

Claude:

- `ANTHROPIC_API_KEY`
- `ANTHROPIC_AUTH_TOKEN`
- `ANTHROPIC_BASE_URL`
- `ANTHROPIC_MODEL`

Codex / OpenAI-compatible:

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`

## Build

```bash
../jetbrains-cc-gui/gradlew.bat compileJava
../jetbrains-cc-gui/gradlew.bat buildPlugin
```

The plugin is compiled against JetBrains Platform `2022.1.4` as the minimum API baseline, declares `2022.1+` compatibility without an upper build cap, and uses compatibility fallbacks for Commit UI data collection.

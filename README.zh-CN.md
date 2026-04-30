# AI Commit Message Generator

[English](README.md) | [简体中文](README.zh-CN.md)

一个独立的 JetBrains IDE 插件 MVP，用于根据 Commit 工具窗口中当前选中的提交文件生成 Git commit message。

## MVP 范围

- 在 Commit Message 工具栏中新增 AI 生成按钮。
- 只使用当前选中准备提交的文件。
- 生成后直接写入 Commit Message 输入框。
- 生成过程中显示加载图标。
- 默认 60 秒超时。
- 生成失败时，将错误提示写入 Commit Message 输入框。
- 支持 Claude 和 Codex/OpenAI 兼容中转站。
- 设置界面支持中文/英文切换。
- 支持通过手动 Base URL 配置 OpenAI 兼容或 Claude 兼容的第三方中转站。
- 凭证解析优先级：
  - 手动设置
  - 本地 Claude/Codex 配置
  - 只读 cc-switch 配置
  - 环境变量
- 内置 `git-commit` Skill 作为默认 commit message 风格规范。
- 按当前提供方扫描本地 Skills，并允许用户覆盖内置默认规范。
- 在发送 diff 给 AI 前过滤敏感路径。

## 设置

打开 `Settings | Tools | AI Commit Message`。

手动填写的 API Key 会存储在 JetBrains PasswordSafe 中。Base URL、模型、语言、超时时间、Skill 和排除规则会存储在插件设置文件中。

未选择本地 Skill 时，插件会使用内置的 `git-commit` Skill。该规范专门适配插件场景：只根据选中 diff 生成一条 Conventional Commits 信息，遵守配置的输出语言，并明确禁止 AI 执行命令、提交或推送。

Codex/OpenAI 兼容中转站的 Base URL 支持填写根地址、`/v1`、`/v1/chat/completions` 或 `/v1/responses`。如果中转站走 Codex/Responses 协议，可在 Wire API 中选择 Responses。

手动配置留空时，插件会尝试读取本地工具配置：

- Claude：`~/.claude/settings.json`、`~/.claude/settings.local.json`
- Codex：`~/.codex/config.toml`，包括 `model_provider`、`model`、`base_url` 和 `wire_api`

## 环境变量

Claude：

- `ANTHROPIC_API_KEY`
- `ANTHROPIC_AUTH_TOKEN`
- `ANTHROPIC_BASE_URL`
- `ANTHROPIC_MODEL`

Codex / OpenAI 兼容：

- `OPENAI_API_KEY`
- `OPENAI_BASE_URL`
- `OPENAI_MODEL`

## 构建

```bash
../jetbrains-cc-gui/gradlew.bat compileJava
../jetbrains-cc-gui/gradlew.bat buildPlugin
```

插件以 JetBrains Platform `2022.1.4` 作为最低 API 编译基线，声明兼容 `2022.1+` 且不设置版本上限，并通过兼容 fallback 获取 Commit UI 数据。

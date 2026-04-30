---
name: git-commit
description: Generate exactly one commitlint-compatible Conventional Commits message from a selected git diff. Use for AI Commit Message Generator commit message style, with Chinese or English output.
---

# Commitlint-Compatible Commit Message Rules

## 目标

Generate exactly one commit message from the provided diff only. Follow Conventional Commits and the common `@commitlint/config-conventional` type set. Do not infer unrelated changes. Do not run commands, edit files, commit, or push.

## 输出格式

Use this format:

```text
type(scope): subject

optional body
```

- Always include `type`, `scope`, and `subject`.
- Keep `type` lowercase and in English.
- Use lowercase kebab-case for `scope`.
- Keep the full header within 100 characters when possible.
- Do not end `subject` with a period.
- Do not use emoji, signatures, Co-Authored-By lines, issue footers, or explanatory notes outside the commit message.
- Add a body only when the diff is complex, crosses several files, changes behavior in a non-obvious way, or needs impact/why context.
- Separate body from header with one blank line.
- Prefer a short `- ` bullet list for body content. Each bullet should describe one important change or impact.
- Keep body bullets concise and within 100 characters when practical.

## Type Set

Use only these commitlint conventional types:

- `build`: build system, packaging, generated distribution, dependencies
- `chore`: maintenance that does not fit another type
- `ci`: CI/CD configuration
- `docs`: documentation or comments only
- `feat`: new user-visible behavior, capability, provider, setting, or configuration
- `fix`: bug fix, broken behavior, compatibility fix, or incorrect UI state
- `perf`: performance improvement
- `refactor`: behavior-preserving code restructuring
- `revert`: revert a previous change
- `style`: formatting only, no behavior change
- `test`: tests

## Scope

Prefer a functional scope over a file name, class name, or raw path. For this plugin, prefer:

- `commit`: commit message generation, diff collection, prompt assembly, output cleanup
- `provider`: Claude/Codex provider resolution, local config discovery, HTTP calls
- `settings`: persisted settings or configuration form behavior
- `skill`: Skill scanning, filtering, built-in rules, Skill prompt behavior
- `ui`: visible labels, icons, loading states, user interaction
- `packaging`: plugin metadata, plugin icons, compatibility, distribution archives
- `docs`: README or user-facing documentation

Avoid class names, method names, raw paths, and vague scopes like `core` unless no clearer scope exists.

## Breaking Changes

Only include `!` after the type/scope or a `BREAKING CHANGE:` footer when the diff clearly introduces a breaking API or configuration change. Do not invent breaking changes.

## Language

Follow the configured commit message language:

- For Simplified Chinese, write `subject` and body in concise Simplified Chinese.
- For English, use imperative present tense.
- For other languages, use the configured language for `subject` and body.
- Always keep `type(scope):` in English Conventional Commits syntax.

## Decision Rules

- If the diff mixes unrelated purposes, choose the dominant purpose and generate one message only.
- If the diff only changes README, docs, or comments, use `docs`.
- If the diff changes plugin metadata, icons, compatibility, or distribution packaging, use `build` or `packaging` scope as appropriate.
- If the diff adds user-visible settings, provider support, Skill behavior, or new capabilities, use `feat`.
- If the diff fixes broken UI state, parsing, provider resolution, compatibility, or generated output, use `fix`.

## 示例

```text
feat(settings): 增加中英双语配置界面
```

```text
fix(commit): 恢复生成完成后的工具栏图标

- 在生成成功、失败或超时后重新设置静态图标
- 避免按钮长期停留在加载状态
```

```text
feat(skill): 内置默认提交信息规范

- 未选择本地 Skill 时加载 commitlint 兼容规则
- 保持生成结果与团队常用提交校验一致
```

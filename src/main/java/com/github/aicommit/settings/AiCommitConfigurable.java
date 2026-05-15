package com.github.aicommit.settings;

import com.github.aicommit.AiCommitBundle;
import com.github.aicommit.provider.EffectiveProviderConfig;
import com.github.aicommit.provider.ProviderConfigResolver;
import com.github.aicommit.provider.ProviderKind;
import com.github.aicommit.skill.SkillInfo;
import com.github.aicommit.skill.SkillScanner;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiCommitConfigurable implements Configurable {
    private static final int SKILL_COMBO_MIN_WIDTH = 260;
    private static final int SKILL_COMBO_MAX_WIDTH = 420;
    private static final int SKILL_COMBO_PADDING = 48;
    private static final String[] CLAUDE_MODELS = {
            AiCommitSettings.DEFAULT_CLAUDE_MODEL,
            "claude-opus-4-6",
            "claude-sonnet-4-6",
            "claude-haiku-4-5-20251001"
    };
    private static final String[] OPENAI_MODELS = {
            AiCommitSettings.DEFAULT_OPENAI_MODEL,
            "gpt-5.4",
            "gpt-5.4-mini",
            "gpt-5.3-codex",
            "gpt-5.2"
    };

    private ComboBox<String> uiLanguageCombo;
    private ComboBox<String> providerCombo;
    private ComboBox<String> languageCombo;
    private ComboBox<String> skillCombo;
    private JSpinner timeoutSpinner;
    private JBCheckBox sensitiveFilterCheckBox;
    private JBTextArea excludePatternsArea;
    private JBPasswordField claudeApiKey;
    private JBTextField claudeBaseUrl;
    private ComboBox<String> claudeModelCombo;
    private JBTextField claudeCustomModel;
    private JBPasswordField openaiApiKey;
    private JBTextField openaiBaseUrl;
    private ComboBox<String> openaiModelCombo;
    private JBTextField openaiCustomModel;
    private ComboBox<String> openaiWireApiCombo;
    private JButton refreshSkillsButton;
    private JButton restoreDefaultsButton;
    private JLabel uiLanguageLabel;
    private JLabel providerLabel;
    private JLabel languageLabel;
    private JLabel skillLabel;
    private JLabel timeoutLabel;
    private JLabel excludedPathsLabel;
    private JLabel priorityHintLabel;
    private JLabel claudeSectionLabel;
    private JLabel claudeApiKeyLabel;
    private JLabel claudeBaseUrlLabel;
    private JLabel claudeModelLabel;
    private JLabel claudeCustomModelLabel;
    private JLabel openaiSectionLabel;
    private JLabel openaiApiKeyLabel;
    private JLabel openaiBaseUrlLabel;
    private JLabel openaiModelLabel;
    private JLabel openaiCustomModelLabel;
    private JLabel openaiWireApiLabel;
    private JPanel panel;
    private final Map<String, String> providerLabelsToIds = new LinkedHashMap<>();
    private final Map<String, String> skillLabelsToRefs = new LinkedHashMap<>();

    @Override
    public @Nls String getDisplayName() {
        return AiCommitBundle.message("settings.displayName");
    }

    @Override
    public @Nullable JComponent createComponent() {
        AiCommitSettings.State state = AiCommitSettings.getInstance().copyState();
        applyLocalToolConfigToState(state, false);

        uiLanguageCombo = new ComboBox<>(new String[]{
                AiCommitSettings.UI_LANGUAGE_EN, AiCommitSettings.UI_LANGUAGE_ZH
        });
        providerCombo = new ComboBox<>();
        languageCombo = new ComboBox<>(languageDisplayNames());
        skillCombo = new ComboBox<>();
        configureSkillComboSize();
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(75, 10, 120, 5));
        sensitiveFilterCheckBox = new JBCheckBox();
        excludePatternsArea = new JBTextArea(8, 42);
        claudeApiKey = new JBPasswordField();
        claudeBaseUrl = new JBTextField();
        claudeModelCombo = new ComboBox<>();
        claudeCustomModel = new JBTextField();
        openaiApiKey = new JBPasswordField();
        openaiBaseUrl = new JBTextField();
        openaiModelCombo = new ComboBox<>();
        openaiCustomModel = new JBTextField();
        openaiWireApiCombo = new ComboBox<>(new String[]{"Chat Completions", "Responses"});

        refreshSkillsButton = new JButton();
        refreshSkillsButton.addActionListener(event -> loadSkills(getCurrentProjectBasePath(), true));
        restoreDefaultsButton = new JButton();
        restoreDefaultsButton.addActionListener(event -> restoreProviderDefaultsFromLocalConfig());
        uiLanguageCombo.addActionListener(event -> {
            updateTexts();
            loadSkills(getCurrentProjectBasePath(), false);
        });
        providerCombo.addActionListener(event -> loadSkills(getCurrentProjectBasePath(), false));
        claudeModelCombo.addActionListener(event ->
                updateModelInputState(claudeModelCombo, claudeCustomModel, claudeCustomModelLabel));
        openaiModelCombo.addActionListener(event ->
                updateModelInputState(openaiModelCombo, openaiCustomModel, openaiCustomModelLabel));

        uiLanguageLabel = new JBLabel();
        providerLabel = new JBLabel();
        languageLabel = new JBLabel();
        skillLabel = new JBLabel();
        timeoutLabel = new JBLabel();
        excludedPathsLabel = new JBLabel();
        priorityHintLabel = new JBLabel();
        claudeSectionLabel = new JBLabel();
        claudeApiKeyLabel = new JBLabel();
        claudeBaseUrlLabel = new JBLabel();
        claudeModelLabel = new JBLabel();
        claudeCustomModelLabel = new JBLabel();
        openaiSectionLabel = new JBLabel();
        openaiApiKeyLabel = new JBLabel();
        openaiBaseUrlLabel = new JBLabel();
        openaiModelLabel = new JBLabel();
        openaiCustomModelLabel = new JBLabel();
        openaiWireApiLabel = new JBLabel();

        applyStateToUi(state);
        loadSkills(getCurrentProjectBasePath(), false);
        updateTexts();

        FormBuilder builder = FormBuilder.createFormBuilder()
                .setHorizontalGap(8)
                .addLabeledComponent(uiLanguageLabel, uiLanguageCombo, 1, false)
                .addLabeledComponent(providerLabel, providerCombo, 1, false)
                .addLabeledComponent(languageLabel, languageCombo, 1, false)
                .addLabeledComponent(skillLabel, skillCombo, 1, false)
                .addComponent(refreshSkillsButton)
                .addLabeledComponent(timeoutLabel, timeoutSpinner, 1, false)
                .addComponent(sensitiveFilterCheckBox)
                .addLabeledComponent(excludedPathsLabel, new JBScrollPane(excludePatternsArea), 1, true)
                .addSeparator()
                .addComponent(priorityHintLabel)
                .addComponent(restoreDefaultsButton)
                .addSeparator()
                .addComponent(claudeSectionLabel)
                .addLabeledComponent(claudeApiKeyLabel, claudeApiKey, 1, false)
                .addLabeledComponent(claudeBaseUrlLabel, claudeBaseUrl, 1, false)
                .addLabeledComponent(claudeModelLabel, claudeModelCombo, 1, false)
                .addLabeledComponent(claudeCustomModelLabel, claudeCustomModel, 1, false)
                .addSeparator()
                .addComponent(openaiSectionLabel)
                .addLabeledComponent(openaiApiKeyLabel, openaiApiKey, 1, false)
                .addLabeledComponent(openaiBaseUrlLabel, openaiBaseUrl, 1, false)
                .addLabeledComponent(openaiModelLabel, openaiModelCombo, 1, false)
                .addLabeledComponent(openaiCustomModelLabel, openaiCustomModel, 1, false)
                .addLabeledComponent(openaiWireApiLabel, openaiWireApiCombo, 1, false)
                .addComponentFillVertically(new JPanel(), 0);

        panel = new JPanel(new BorderLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        panel.add(new JBScrollPane(builder.getPanel()), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public boolean isModified() {
        AiCommitSettings.State current = AiCommitSettings.getInstance().copyState();
        applyLocalToolConfigToState(current, false);
        AiCommitSettings.State ui = readStateFromUi();
        return !current.uiLanguage.equals(ui.uiLanguage)
                || !current.provider.equals(ui.provider)
                || !current.language.equals(ui.language)
                || !current.skillRef.equals(ui.skillRef)
                || current.timeoutSeconds != ui.timeoutSeconds
                || current.enableSensitiveFilter != ui.enableSensitiveFilter
                || !current.excludePatterns.equals(ui.excludePatterns)
                || !sameProvider(current.claude, ui.claude)
                || !sameProvider(current.openai, ui.openai);
    }

    @Override
    public void apply() {
        AiCommitSettings.getInstance().update(readStateFromUi());
    }

    @Override
    public void reset() {
        AiCommitSettings.State state = AiCommitSettings.getInstance().copyState();
        applyLocalToolConfigToState(state, false);
        applyStateToUi(state);
    }

    private void applyStateToUi(AiCommitSettings.State state) {
        uiLanguageCombo.setSelectedItem(state.uiLanguage);
        loadProviderItems(state.provider);
        languageCombo.setSelectedItem(CommitLanguage.displayName(state.language));
        timeoutSpinner.setValue(state.timeoutSeconds);
        sensitiveFilterCheckBox.setSelected(state.enableSensitiveFilter);
        excludePatternsArea.setText(state.excludePatterns);
        setPasswordText(claudeApiKey, state.claude.apiKey);
        claudeBaseUrl.setText(state.claude.baseUrl);
        loadModelItems(claudeModelCombo, claudeCustomModel, state.claude.model, CLAUDE_MODELS);
        setPasswordText(openaiApiKey, state.openai.apiKey);
        openaiBaseUrl.setText(state.openai.baseUrl);
        loadModelItems(openaiModelCombo, openaiCustomModel, state.openai.model, OPENAI_MODELS);
        openaiWireApiCombo.setSelectedItem(wireApiLabel(state.openai.wireApi));
        selectSkill(state.skillRef);
    }

    private void applyLocalToolConfigToState(AiCommitSettings.State state, boolean force) {
        if (!force && state.providerConfigSaved) {
            return;
        }
        ProviderConfigResolver resolver = new ProviderConfigResolver();
        applyLocalProviderConfig(state.claude, resolver.resolveLocalToolConfig(ProviderKind.CLAUDE), force,
                AiCommitSettings.DEFAULT_CLAUDE_MODEL);
        applyLocalProviderConfig(state.openai, resolver.resolveLocalToolConfig(ProviderKind.OPENAI), force,
                AiCommitSettings.DEFAULT_OPENAI_MODEL);
    }

    private void applyLocalProviderConfig(AiCommitSettings.ProviderState target, EffectiveProviderConfig local,
                                          boolean force,
                                          String defaultModel) {
        if (target == null || local == null) {
            return;
        }
        if ((force || isBlank(target.apiKey)) && !isBlank(local.getApiKey())) {
            target.apiKey = local.getApiKey();
        }
        if ((force || isBlank(target.baseUrl)) && !isBlank(local.getBaseUrl())) {
            target.baseUrl = local.getBaseUrl();
        }
        if ((force || isBlank(target.model) || defaultModel.equals(target.model)) && !isBlank(local.getModel())) {
            target.model = local.getModel();
        }
        if ((force || isBlank(target.wireApi)) && !isBlank(local.getWireApi())) {
            target.wireApi = local.getWireApi();
        }
    }

    private void restoreProviderDefaultsFromLocalConfig() {
        AiCommitSettings.State state = readStateFromUi();
        state.claude = new AiCommitSettings.ProviderState(AiCommitSettings.DEFAULT_CLAUDE_MODEL);
        state.openai = new AiCommitSettings.ProviderState(AiCommitSettings.DEFAULT_OPENAI_MODEL);
        applyLocalToolConfigToState(state, true);
        applyStateToUi(state);
    }

    private AiCommitSettings.State readStateFromUi() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.uiLanguage = stringValue(uiLanguageCombo.getSelectedItem());
        state.provider = providerLabelsToIds.getOrDefault(stringValue(providerCombo.getSelectedItem()),
                AiCommitSettings.PROVIDER_AUTO);
        state.language = CommitLanguage.valueFromDisplayName(stringValue(languageCombo.getSelectedItem()));
        state.skillRef = skillLabelsToRefs.getOrDefault(stringValue(skillCombo.getSelectedItem()), "");
        state.timeoutSeconds = ((Number) timeoutSpinner.getValue()).intValue();
        state.enableSensitiveFilter = sensitiveFilterCheckBox.isSelected();
        state.excludePatterns = excludePatternsArea.getText();
        state.claude.apiKey = new String(claudeApiKey.getPassword()).trim();
        state.claude.baseUrl = claudeBaseUrl.getText().trim();
        state.claude.model = readModelFromUi(claudeModelCombo, claudeCustomModel);
        state.openai.apiKey = new String(openaiApiKey.getPassword()).trim();
        state.openai.baseUrl = openaiBaseUrl.getText().trim();
        state.openai.model = readModelFromUi(openaiModelCombo, openaiCustomModel);
        state.openai.wireApi = wireApiValue(stringValue(openaiWireApiCombo.getSelectedItem()));
        state.normalize();
        return state;
    }

    private void loadSkills(String projectBasePath, boolean forceRefresh) {
        String selectedLabel = skillCombo == null ? "" : stringValue(skillCombo.getSelectedItem());
        String selectedRef = skillLabelsToRefs.getOrDefault(selectedLabel, "");
        skillLabelsToRefs.clear();
        skillLabelsToRefs.put(builtInSkillLabel(), "");
        if (skillCombo != null) {
            skillCombo.removeAllItems();
            skillCombo.addItem(builtInSkillLabel());
        }
        List<String> labels = new ArrayList<>();
        labels.add(builtInSkillLabel());
        List<SkillInfo> skills = new SkillScanner().scan(projectBasePath, selectedProviderId(), forceRefresh);
        for (SkillInfo skill : skills) {
            String label = uniqueSkillLabel(skillLabel(skill), skill.getPath());
            skillLabelsToRefs.put(label, skill.getPath());
            labels.add(label);
            if (skillCombo != null) {
                skillCombo.addItem(label);
            }
        }
        if (skillCombo != null) {
            String selected = selectSkillRef(selectedRef);
            if (selected != null && !labels.contains(selected)) {
                labels.add(selected);
            }
            configureSkillComboSize(labels);
            skillCombo.revalidate();
            skillCombo.repaint();
        }
    }

    private void loadSkills(String projectBasePath) {
        loadSkills(projectBasePath, false);
    }

    private String selectSkillRef(String skillRef) {
        if (skillRef == null || skillRef.trim().isEmpty()) {
            String label = builtInSkillLabel();
            skillCombo.setSelectedItem(label);
            return label;
        }
        for (Map.Entry<String, String> entry : skillLabelsToRefs.entrySet()) {
            if (skillRef.equals(entry.getValue())) {
                skillCombo.setSelectedItem(entry.getKey());
                return entry.getKey();
            }
        }
        String label = "(Configured) " + skillRef;
        skillLabelsToRefs.put(label, skillRef);
        skillCombo.addItem(label);
        skillCombo.setSelectedItem(label);
        return label;
    }

    private void loadProviderItems(String selectedProviderId) {
        String selected = selectedProviderId == null ? AiCommitSettings.PROVIDER_AUTO : selectedProviderId;
        providerLabelsToIds.clear();
        providerCombo.removeAllItems();
        addProviderItem(providerAutoLabel(), AiCommitSettings.PROVIDER_AUTO);
        addProviderItem(claudeProviderLabel(), AiCommitSettings.PROVIDER_CLAUDE);
        addProviderItem(codexProviderLabel(), AiCommitSettings.PROVIDER_OPENAI);
        for (Map.Entry<String, String> entry : providerLabelsToIds.entrySet()) {
            if (selected.equals(entry.getValue())) {
                providerCombo.setSelectedItem(entry.getKey());
                return;
            }
        }
        providerCombo.setSelectedIndex(0);
    }

    private void addProviderItem(String label, String id) {
        providerLabelsToIds.put(label, id);
        providerCombo.addItem(label);
    }

    private void updateTexts() {
        String selectedProvider = providerLabelsToIds.getOrDefault(stringValue(providerCombo.getSelectedItem()),
                AiCommitSettings.PROVIDER_AUTO);
        String selectedClaudeModel = readModelFromUi(claudeModelCombo, claudeCustomModel);
        String selectedOpenAiModel = readModelFromUi(openaiModelCombo, openaiCustomModel);
        uiLanguageLabel.setText(zh() ? "界面语言" : "Interface language");
        providerLabel.setText(zh() ? "AI 提供方" : "Provider");
        languageLabel.setText(zh() ? "提交信息语言" : "Commit message language");
        skillLabel.setText(zh() ? "本地 Skill" : "Local Skill");
        refreshSkillsButton.setText(zh() ? "刷新 Skills" : "Refresh Skills");
        restoreDefaultsButton.setText(zh() ? "恢复默认（读取本地配置）" : "Restore defaults from local config");
        timeoutLabel.setText(zh() ? "超时时间（秒）" : "Timeout seconds");
        sensitiveFilterCheckBox.setText(zh()
                ? "发送给 AI 前过滤敏感文件"
                : "Filter sensitive files before sending diff to AI");
        excludedPathsLabel.setText(zh() ? "排除路径/匹配规则" : "Excluded paths/patterns");
        priorityHintLabel.setText(zh()
                ? "手动配置优先级最高。留空时依次读取本地 Claude/Codex 配置和环境变量。"
                : "Manual provider config has highest priority. Empty fields fall back to local Claude/Codex config, then environment variables.");
        claudeSectionLabel.setText(claudeProviderLabel());
        claudeApiKeyLabel.setText("API Key / Auth Token");
        claudeBaseUrlLabel.setText("Base URL");
        claudeModelLabel.setText(zh() ? "模型" : "Model");
        claudeCustomModelLabel.setText(zh() ? "自定义模型 ID（可填写任意兼容模型）"
                : "Custom model ID, any compatible model");
        openaiSectionLabel.setText(codexProviderLabel());
        openaiApiKeyLabel.setText("API Key");
        openaiBaseUrlLabel.setText("Base URL");
        openaiModelLabel.setText(zh() ? "模型" : "Model");
        openaiCustomModelLabel.setText(zh() ? "自定义模型 ID（可填写任意兼容模型）"
                : "Custom model ID, any compatible model");
        openaiWireApiLabel.setText(zh() ? "Wire API（Codex 中转站常用 Responses）" : "Wire API, Codex relays often use Responses");
        loadProviderItems(selectedProvider);
        loadModelItems(claudeModelCombo, claudeCustomModel, selectedClaudeModel, CLAUDE_MODELS);
        loadModelItems(openaiModelCombo, openaiCustomModel, selectedOpenAiModel, OPENAI_MODELS);
    }

    private boolean zh() {
        return AiCommitSettings.UI_LANGUAGE_ZH.equals(stringValue(uiLanguageCombo.getSelectedItem()));
    }

    private String providerAutoLabel() {
        return zh() ? "自动选择" : "Auto";
    }

    private String claudeProviderLabel() {
        return zh() ? "Claude / Claude 兼容中转站" : "Claude / Claude relay";
    }

    private String codexProviderLabel() {
        return zh() ? "Codex / OpenAI 兼容中转站" : "Codex / OpenAI-compatible relay";
    }

    private String selectedProviderId() {
        return providerLabelsToIds.getOrDefault(stringValue(providerCombo.getSelectedItem()),
                AiCommitSettings.PROVIDER_AUTO);
    }

    private String skillProviderLabel(SkillInfo skill) {
        if (AiCommitSettings.PROVIDER_CLAUDE.equals(skill.getProviderId())) {
            return "[Claude]";
        }
        if (AiCommitSettings.PROVIDER_OPENAI.equals(skill.getProviderId())) {
            return "[Codex]";
        }
        return "[Skill]";
    }

    private String skillLabel(SkillInfo skill) {
        return skillProviderLabel(skill) + " " + skill.getName();
    }

    private String uniqueSkillLabel(String baseLabel, String skillPath) {
        if (!skillLabelsToRefs.containsKey(baseLabel)) {
            return baseLabel;
        }
        String suffix = compactSkillSource(skillPath);
        String candidate = baseLabel + " · " + suffix;
        int index = 2;
        while (skillLabelsToRefs.containsKey(candidate)) {
            candidate = baseLabel + " · " + suffix + " " + index;
            index++;
        }
        return candidate;
    }

    private String compactSkillSource(String skillPath) {
        String normalized = skillPath == null ? "" : skillPath.replace('\\', '/').toLowerCase();
        String projectBasePath = getCurrentProjectBasePath();
        if (projectBasePath != null && normalized.startsWith(projectBasePath.replace('\\', '/').toLowerCase())) {
            return "project";
        }
        return "user";
    }

    private String builtInSkillLabel() {
        return zh() ? "内置默认：git-commit" : "Built-in default: git-commit";
    }

    private void loadModelItems(ComboBox<String> combo, JBTextField customModel, String selectedModel,
                                String[] presetModels) {
        String model = selectedModel == null ? "" : selectedModel.trim();
        combo.removeAllItems();
        for (String presetModel : presetModels) {
            combo.addItem(presetModel);
        }
        combo.addItem(otherModelLabel());
        if (isPresetModel(model, presetModels)) {
            combo.setSelectedItem(model);
            customModel.setText("");
        } else {
            combo.setSelectedItem(otherModelLabel());
            customModel.setText(model);
        }
        updateModelInputState(combo, customModel, customModelLabel(combo));
    }

    private String readModelFromUi(ComboBox<String> combo, JBTextField customModel) {
        String selected = stringValue(combo.getSelectedItem());
        if (isOtherModelLabel(selected)) {
            return customModel.getText().trim();
        }
        return selected.trim();
    }

    private void updateModelInputState(ComboBox<String> combo, JBTextField customModel, JLabel customModelLabel) {
        boolean custom = isOtherModelLabel(stringValue(combo.getSelectedItem()));
        customModel.setEnabled(custom);
        customModel.setVisible(custom);
        if (customModelLabel != null) {
            customModelLabel.setVisible(custom);
        }
        if (panel != null) {
            panel.revalidate();
            panel.repaint();
        }
    }

    private JLabel customModelLabel(ComboBox<String> combo) {
        if (combo == claudeModelCombo) {
            return claudeCustomModelLabel;
        }
        if (combo == openaiModelCombo) {
            return openaiCustomModelLabel;
        }
        return null;
    }

    private boolean isPresetModel(String model, String[] presetModels) {
        for (String presetModel : presetModels) {
            if (presetModel.equals(model)) {
                return true;
            }
        }
        return false;
    }

    private String otherModelLabel() {
        return zh() ? "（自定义）其他模型" : "(Custom) other model";
    }

    private boolean isOtherModelLabel(String label) {
        return "（自定义）其他模型".equals(label)
                || "(Custom) other model".equals(label);
    }

    private void configureSkillComboSize() {
        List<String> labels = new ArrayList<>();
        labels.add(builtInSkillLabel());
        configureSkillComboSize(labels);
    }

    private void configureSkillComboSize(List<String> labels) {
        String widest = widestLabel(labels);
        skillCombo.setPrototypeDisplayValue(widest);
        Dimension preferred = skillCombo.getPreferredSize();
        int height = preferred == null || preferred.height <= 0 ? 30 : preferred.height;
        int measured = measuredTextWidth(widest) + SKILL_COMBO_PADDING;
        int width = Math.min(Math.max(measured, SKILL_COMBO_MIN_WIDTH), SKILL_COMBO_MAX_WIDTH);
        Dimension size = new Dimension(width, height);
        skillCombo.setMinimumSize(size);
        skillCombo.setPreferredSize(size);
        skillCombo.setMaximumRowCount(12);
    }

    private String widestLabel(List<String> labels) {
        String widest = builtInSkillLabel();
        if (labels == null) {
            return widest;
        }
        for (String label : labels) {
            if (label != null && label.length() > widest.length()) {
                widest = label;
            }
        }
        return widest;
    }

    private int measuredTextWidth(String value) {
        FontMetrics metrics = skillCombo.getFontMetrics(skillCombo.getFont());
        return metrics == null ? 0 : metrics.stringWidth(value == null ? "" : value);
    }

    private void selectSkill(String skillRef) {
        selectSkillRef(skillRef);
    }

    private String getCurrentProjectBasePath() {
        com.intellij.openapi.project.Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0 || projects[0].getBasePath() == null) {
            return null;
        }
        return projects[0].getBasePath();
    }

    private boolean sameProvider(AiCommitSettings.ProviderState left, AiCommitSettings.ProviderState right) {
        return left.apiKey.equals(right.apiKey)
                && left.baseUrl.equals(right.baseUrl)
                && left.model.equals(right.model)
                && left.wireApi.equals(right.wireApi);
    }

    private void setPasswordText(JBPasswordField field, String value) {
        field.setText(value == null ? "" : value);
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String[] languageDisplayNames() {
        String[] values = CommitLanguage.values();
        String[] displayNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            displayNames[i] = CommitLanguage.displayName(values[i]);
        }
        return displayNames;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String wireApiValue(String label) {
        return "Responses".equals(label) ? "responses" : "";
    }

    private String wireApiLabel(String value) {
        return "responses".equalsIgnoreCase(value == null ? "" : value.trim())
                ? "Responses"
                : "Chat Completions";
    }
}

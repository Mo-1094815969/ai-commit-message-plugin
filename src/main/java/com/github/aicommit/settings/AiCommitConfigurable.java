package com.github.aicommit.settings;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AiCommitConfigurable implements Configurable {
    private static final int SKILL_COMBO_WIDTH = 560;
    private static final String SKILL_COMBO_PROTOTYPE =
            "[Codex] built-in-git-commit-style - C:\\Users\\user\\.codex\\skills\\git-commit\\SKILL.md";

    private ComboBox<String> uiLanguageCombo;
    private ComboBox<String> providerCombo;
    private ComboBox<String> languageCombo;
    private ComboBox<String> skillCombo;
    private JSpinner timeoutSpinner;
    private JBCheckBox sensitiveFilterCheckBox;
    private JBTextArea excludePatternsArea;
    private JBPasswordField claudeApiKey;
    private JBTextField claudeBaseUrl;
    private JBTextField claudeModel;
    private JBPasswordField openaiApiKey;
    private JBTextField openaiBaseUrl;
    private JBTextField openaiModel;
    private ComboBox<String> openaiWireApiCombo;
    private JButton refreshSkillsButton;
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
    private JLabel openaiSectionLabel;
    private JLabel openaiApiKeyLabel;
    private JLabel openaiBaseUrlLabel;
    private JLabel openaiModelLabel;
    private JLabel openaiWireApiLabel;
    private JPanel panel;
    private final Map<String, String> providerLabelsToIds = new LinkedHashMap<>();
    private final Map<String, String> skillLabelsToRefs = new LinkedHashMap<>();

    @Override
    public @Nls String getDisplayName() {
        return "AI Commit Message / AI 提交信息";
    }

    @Override
    public @Nullable JComponent createComponent() {
        AiCommitSettings.State state = AiCommitSettings.getInstance().copyState();

        uiLanguageCombo = new ComboBox<>(new String[]{
                AiCommitSettings.UI_LANGUAGE_EN, AiCommitSettings.UI_LANGUAGE_ZH
        });
        providerCombo = new ComboBox<>();
        languageCombo = new ComboBox<>(new String[]{
                "English", "Simplified Chinese", "Traditional Chinese", "Japanese", "Korean", "Spanish", "French", "German"
        });
        skillCombo = new ComboBox<>();
        configureSkillComboSize();
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(60, 10, 120, 5));
        sensitiveFilterCheckBox = new JBCheckBox();
        excludePatternsArea = new JBTextArea(8, 42);
        claudeApiKey = new JBPasswordField();
        claudeBaseUrl = new JBTextField();
        claudeModel = new JBTextField();
        openaiApiKey = new JBPasswordField();
        openaiBaseUrl = new JBTextField();
        openaiModel = new JBTextField();
        openaiWireApiCombo = new ComboBox<>(new String[]{"Chat Completions", "Responses"});

        refreshSkillsButton = new JButton();
        refreshSkillsButton.addActionListener(event -> loadSkills(getCurrentProjectBasePath()));
        uiLanguageCombo.addActionListener(event -> {
            updateTexts();
            loadSkills(getCurrentProjectBasePath());
        });
        providerCombo.addActionListener(event -> loadSkills(getCurrentProjectBasePath()));

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
        openaiSectionLabel = new JBLabel();
        openaiApiKeyLabel = new JBLabel();
        openaiBaseUrlLabel = new JBLabel();
        openaiModelLabel = new JBLabel();
        openaiWireApiLabel = new JBLabel();

        applyStateToUi(state);
        loadSkills(getCurrentProjectBasePath());
        updateTexts();

        FormBuilder builder = FormBuilder.createFormBuilder()
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
                .addSeparator()
                .addComponent(claudeSectionLabel)
                .addLabeledComponent(claudeApiKeyLabel, claudeApiKey, 1, false)
                .addLabeledComponent(claudeBaseUrlLabel, claudeBaseUrl, 1, false)
                .addLabeledComponent(claudeModelLabel, claudeModel, 1, false)
                .addSeparator()
                .addComponent(openaiSectionLabel)
                .addLabeledComponent(openaiApiKeyLabel, openaiApiKey, 1, false)
                .addLabeledComponent(openaiBaseUrlLabel, openaiBaseUrl, 1, false)
                .addLabeledComponent(openaiModelLabel, openaiModel, 1, false)
                .addLabeledComponent(openaiWireApiLabel, openaiWireApiCombo, 1, false)
                .addComponentFillVertically(new JPanel(), 0);

        panel = new JPanel(new BorderLayout());
        panel.add(new JBScrollPane(builder.getPanel()), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public boolean isModified() {
        AiCommitSettings.State current = AiCommitSettings.getInstance().copyState();
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
        applyStateToUi(AiCommitSettings.getInstance().copyState());
    }

    private void applyStateToUi(AiCommitSettings.State state) {
        uiLanguageCombo.setSelectedItem(state.uiLanguage);
        loadProviderItems(state.provider);
        languageCombo.setSelectedItem(state.language);
        timeoutSpinner.setValue(state.timeoutSeconds);
        sensitiveFilterCheckBox.setSelected(state.enableSensitiveFilter);
        excludePatternsArea.setText(state.excludePatterns);
        setPasswordText(claudeApiKey, state.claude.apiKey);
        claudeBaseUrl.setText(state.claude.baseUrl);
        claudeModel.setText(state.claude.model);
        setPasswordText(openaiApiKey, state.openai.apiKey);
        openaiBaseUrl.setText(state.openai.baseUrl);
        openaiModel.setText(state.openai.model);
        openaiWireApiCombo.setSelectedItem(wireApiLabel(state.openai.wireApi));
        selectSkill(state.skillRef);
    }

    private AiCommitSettings.State readStateFromUi() {
        AiCommitSettings.State state = new AiCommitSettings.State();
        state.uiLanguage = stringValue(uiLanguageCombo.getSelectedItem());
        state.provider = providerLabelsToIds.getOrDefault(stringValue(providerCombo.getSelectedItem()),
                AiCommitSettings.PROVIDER_AUTO);
        state.language = stringValue(languageCombo.getSelectedItem());
        state.skillRef = skillLabelsToRefs.getOrDefault(stringValue(skillCombo.getSelectedItem()), "");
        state.timeoutSeconds = ((Number) timeoutSpinner.getValue()).intValue();
        state.enableSensitiveFilter = sensitiveFilterCheckBox.isSelected();
        state.excludePatterns = excludePatternsArea.getText();
        state.claude.apiKey = new String(claudeApiKey.getPassword()).trim();
        state.claude.baseUrl = claudeBaseUrl.getText().trim();
        state.claude.model = claudeModel.getText().trim();
        state.openai.apiKey = new String(openaiApiKey.getPassword()).trim();
        state.openai.baseUrl = openaiBaseUrl.getText().trim();
        state.openai.model = openaiModel.getText().trim();
        state.openai.wireApi = wireApiValue(stringValue(openaiWireApiCombo.getSelectedItem()));
        state.normalize();
        return state;
    }

    private void loadSkills(String projectBasePath) {
        String selected = skillCombo == null ? "" : stringValue(skillCombo.getSelectedItem());
        skillLabelsToRefs.clear();
        skillLabelsToRefs.put(builtInSkillLabel(), "");
        if (skillCombo != null) {
            skillCombo.removeAllItems();
            skillCombo.addItem(builtInSkillLabel());
        }
        List<SkillInfo> skills = new SkillScanner().scan(projectBasePath, selectedProviderId());
        for (SkillInfo skill : skills) {
            String label = skillProviderLabel(skill) + " " + skill.getName() + " - " + skill.getPath();
            skillLabelsToRefs.put(label, skill.getPath());
            if (skillCombo != null) {
                skillCombo.addItem(label);
            }
        }
        if (skillCombo != null && skillLabelsToRefs.containsKey(selected)) {
            skillCombo.setSelectedItem(selected);
        }
        if (skillCombo != null) {
            configureSkillComboSize();
            skillCombo.revalidate();
            skillCombo.repaint();
        }
    }

    private void loadProviderItems(String selectedProviderId) {
        String selected = selectedProviderId == null ? AiCommitSettings.PROVIDER_AUTO : selectedProviderId;
        providerLabelsToIds.clear();
        providerCombo.removeAllItems();
        addProviderItem(providerAutoLabel(), AiCommitSettings.PROVIDER_AUTO);
        addProviderItem("Claude", AiCommitSettings.PROVIDER_CLAUDE);
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
        uiLanguageLabel.setText(zh() ? "界面语言" : "Interface language");
        providerLabel.setText(zh() ? "AI 提供方" : "Provider");
        languageLabel.setText(zh() ? "提交信息语言" : "Commit message language");
        skillLabel.setText(zh() ? "本地 Skill" : "Local Skill");
        refreshSkillsButton.setText(zh() ? "刷新 Skills" : "Refresh Skills");
        timeoutLabel.setText(zh() ? "超时时间（秒）" : "Timeout seconds");
        sensitiveFilterCheckBox.setText(zh()
                ? "发送给 AI 前过滤敏感文件"
                : "Filter sensitive files before sending diff to AI");
        excludedPathsLabel.setText(zh() ? "排除路径/匹配规则" : "Excluded paths/patterns");
        priorityHintLabel.setText(zh()
                ? "手动配置优先级最高。留空时依次读取本地 Claude/Codex 配置、cc-switch 配置和环境变量。"
                : "Manual provider config has highest priority. Empty fields fall back to local Claude/Codex config, cc-switch, then environment variables.");
        claudeSectionLabel.setText(zh() ? "Claude / Claude 中转站" : "Claude / Claude relay");
        claudeApiKeyLabel.setText(zh() ? "API Key / Auth Token" : "API Key / Auth Token");
        claudeBaseUrlLabel.setText(zh() ? "Base URL（例如 https://example.com）" : "Base URL, for example https://example.com");
        claudeModelLabel.setText(zh() ? "模型" : "Model");
        openaiSectionLabel.setText(codexProviderLabel());
        openaiApiKeyLabel.setText("API Key");
        openaiBaseUrlLabel.setText(zh()
                ? "Base URL（支持根地址、/v1、/v1/chat/completions、/v1/responses）"
                : "Base URL, supports root, /v1, /v1/chat/completions, /v1/responses");
        openaiModelLabel.setText(zh() ? "模型" : "Model");
        openaiWireApiLabel.setText(zh() ? "Wire API（Codex 中转站常用 Responses）" : "Wire API, Codex relays often use Responses");
        loadProviderItems(selectedProvider);
    }

    private boolean zh() {
        return AiCommitSettings.UI_LANGUAGE_ZH.equals(stringValue(uiLanguageCombo.getSelectedItem()));
    }

    private String providerAutoLabel() {
        return zh() ? "自动选择" : "Auto";
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

    private String builtInSkillLabel() {
        return zh() ? "内置默认：git-commit" : "Built-in default: git-commit";
    }

    private void configureSkillComboSize() {
        skillCombo.setPrototypeDisplayValue(SKILL_COMBO_PROTOTYPE);
        Dimension preferred = skillCombo.getPreferredSize();
        int height = preferred == null || preferred.height <= 0 ? 30 : preferred.height;
        Dimension size = new Dimension(SKILL_COMBO_WIDTH, height);
        skillCombo.setMinimumSize(size);
        skillCombo.setPreferredSize(size);
        skillCombo.setMaximumRowCount(12);
    }

    private void selectSkill(String skillRef) {
        if (skillRef == null || skillRef.trim().isEmpty()) {
            skillCombo.setSelectedItem(builtInSkillLabel());
            return;
        }
        for (Map.Entry<String, String> entry : skillLabelsToRefs.entrySet()) {
            if (skillRef.equals(entry.getValue())) {
                skillCombo.setSelectedItem(entry.getKey());
                return;
            }
        }
        String label = "(Configured) " + skillRef;
        skillLabelsToRefs.put(label, skillRef);
        skillCombo.addItem(label);
        skillCombo.setSelectedItem(label);
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

    private String wireApiValue(String label) {
        return "Responses".equals(label) ? "responses" : "";
    }

    private String wireApiLabel(String value) {
        return "responses".equalsIgnoreCase(value == null ? "" : value.trim())
                ? "Responses"
                : "Chat Completions";
    }
}

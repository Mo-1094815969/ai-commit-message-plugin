package com.github.aicommit.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CommitLanguage {
    public static final String ENGLISH = "English";
    public static final String SIMPLIFIED_CHINESE = "Simplified Chinese";
    public static final String TRADITIONAL_CHINESE = "Traditional Chinese";
    public static final String JAPANESE = "Japanese";
    public static final String KOREAN = "Korean";
    public static final String SPANISH = "Spanish";
    public static final String FRENCH = "French";
    public static final String GERMAN = "German";

    private static final Map<String, String> DISPLAY_NAMES = new LinkedHashMap<>();
    private static final Map<String, String> GENERATING_MESSAGES = new LinkedHashMap<>();

    static {
        DISPLAY_NAMES.put(ENGLISH, "English");
        DISPLAY_NAMES.put(SIMPLIFIED_CHINESE, "简体中文");
        DISPLAY_NAMES.put(TRADITIONAL_CHINESE, "繁體中文");
        DISPLAY_NAMES.put(JAPANESE, "日本語");
        DISPLAY_NAMES.put(KOREAN, "한국어");
        DISPLAY_NAMES.put(SPANISH, "Español");
        DISPLAY_NAMES.put(FRENCH, "Français");
        DISPLAY_NAMES.put(GERMAN, "Deutsch");

        GENERATING_MESSAGES.put(ENGLISH, "Generating commit message...");
        GENERATING_MESSAGES.put(SIMPLIFIED_CHINESE, "正在生成提交信息...");
        GENERATING_MESSAGES.put(TRADITIONAL_CHINESE, "正在產生提交訊息...");
        GENERATING_MESSAGES.put(JAPANESE, "コミットメッセージを生成しています...");
        GENERATING_MESSAGES.put(KOREAN, "커밋 메시지를 생성하는 중...");
        GENERATING_MESSAGES.put(SPANISH, "Generando mensaje de commit...");
        GENERATING_MESSAGES.put(FRENCH, "Génération du message de commit...");
        GENERATING_MESSAGES.put(GERMAN, "Commit-Nachricht wird generiert...");
    }

    private CommitLanguage() {
    }

    public static String[] values() {
        return DISPLAY_NAMES.keySet().toArray(new String[0]);
    }

    public static String displayName(String language) {
        return DISPLAY_NAMES.getOrDefault(normalize(language), DISPLAY_NAMES.get(ENGLISH));
    }

    public static String valueFromDisplayName(String displayName) {
        for (Map.Entry<String, String> entry : DISPLAY_NAMES.entrySet()) {
            if (entry.getValue().equals(displayName) || entry.getKey().equals(displayName)) {
                return entry.getKey();
            }
        }
        return ENGLISH;
    }

    public static String generatingMessage(String language) {
        return GENERATING_MESSAGES.getOrDefault(normalize(language), GENERATING_MESSAGES.get(ENGLISH));
    }

    public static String failureMessage(String language, String reason) {
        String safeReason = reason == null || reason.trim().isEmpty() ? "Unknown error" : reason.trim();
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "生成提交信息失败：" + safeReason;
            case TRADITIONAL_CHINESE:
                return "產生提交訊息失敗：" + safeReason;
            case JAPANESE:
                return "コミットメッセージの生成に失敗しました: " + safeReason;
            case KOREAN:
                return "커밋 메시지 생성 실패: " + safeReason;
            case SPANISH:
                return "Error al generar el mensaje de commit: " + safeReason;
            case FRENCH:
                return "Échec de la génération du message de commit : " + safeReason;
            case GERMAN:
                return "Commit-Nachricht konnte nicht generiert werden: " + safeReason;
            default:
                return "Generation failed: " + safeReason;
        }
    }

    public static String noProjectMessage(String language) {
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "没有打开项目。";
            case TRADITIONAL_CHINESE:
                return "沒有開啟專案。";
            case JAPANESE:
                return "プロジェクトが開かれていません。";
            case KOREAN:
                return "열린 프로젝트가 없습니다.";
            case SPANISH:
                return "No hay ningún proyecto abierto.";
            case FRENCH:
                return "Aucun projet ouvert.";
            case GERMAN:
                return "Kein Projekt geöffnet.";
            default:
                return "No project open.";
        }
    }

    public static String noPanelMessage(String language) {
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "无法访问提交信息输入框。";
            case TRADITIONAL_CHINESE:
                return "無法存取提交訊息輸入框。";
            case JAPANESE:
                return "コミットメッセージ入力欄にアクセスできません。";
            case KOREAN:
                return "커밋 메시지 입력란에 접근할 수 없습니다.";
            case SPANISH:
                return "No se puede acceder al campo del mensaje de commit.";
            case FRENCH:
                return "Impossible d'accéder au champ du message de commit.";
            case GERMAN:
                return "Auf das Eingabefeld für die Commit-Nachricht kann nicht zugegriffen werden.";
            default:
                return "Cannot access input.";
        }
    }

    public static String noChangesMessage(String language) {
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "未选择变更文件。请先在变更列表中选择要生成提交信息的文件。";
            case TRADITIONAL_CHINESE:
                return "未選擇變更檔案。請先在變更清單中選擇要產生提交訊息的檔案。";
            case JAPANESE:
                return "変更ファイルが選択されていません。先に変更一覧で対象ファイルを選択してください。";
            case KOREAN:
                return "변경 파일이 선택되지 않았습니다. 먼저 변경 목록에서 파일을 선택하세요.";
            case SPANISH:
                return "No se seleccionaron archivos modificados. Selecciona archivos en la lista de cambios.";
            case FRENCH:
                return "Aucun fichier modifié sélectionné. Sélectionnez des fichiers dans la liste des changements.";
            case GERMAN:
                return "Keine geänderten Dateien ausgewählt. Wählen Sie zuerst Dateien in der Änderungsliste aus.";
            default:
                return "No file changes selected. Select changes in the Changes list before generating a commit message.";
        }
    }

    public static String alreadyRunningMessage(String language) {
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "提交信息正在生成中。";
            case TRADITIONAL_CHINESE:
                return "提交訊息正在產生中。";
            case JAPANESE:
                return "コミットメッセージを生成中です。";
            case KOREAN:
                return "커밋 메시지를 생성하는 중입니다.";
            case SPANISH:
                return "El mensaje de commit ya se está generando.";
            case FRENCH:
                return "Le message de commit est déjà en cours de génération.";
            case GERMAN:
                return "Die Commit-Nachricht wird bereits generiert.";
            default:
                return "Commit message generation is already running.";
        }
    }

    public static String noDiffMessage(String language) {
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "没有可用的 diff。";
            case TRADITIONAL_CHINESE:
                return "沒有可用的 diff。";
            case JAPANESE:
                return "利用可能な diff がありません。";
            case KOREAN:
                return "사용 가능한 diff가 없습니다.";
            case SPANISH:
                return "No hay ningún diff utilizable.";
            case FRENCH:
                return "Aucun diff exploitable.";
            case GERMAN:
                return "Kein verwendbarer Diff vorhanden.";
            default:
                return "No usable diff.";
        }
    }

    public static String timeoutMessage(String language, int timeoutSeconds) {
        switch (normalize(language)) {
            case SIMPLIFIED_CHINESE:
                return "生成提交信息超时：" + timeoutSeconds + " 秒。";
            case TRADITIONAL_CHINESE:
                return "產生提交訊息逾時：" + timeoutSeconds + " 秒。";
            case JAPANESE:
                return "コミットメッセージの生成がタイムアウトしました: " + timeoutSeconds + " 秒。";
            case KOREAN:
                return "커밋 메시지 생성 시간 초과: " + timeoutSeconds + "초.";
            case SPANISH:
                return "Se agotó el tiempo al generar el mensaje de commit: " + timeoutSeconds + " s.";
            case FRENCH:
                return "Délai dépassé pour générer le message de commit : " + timeoutSeconds + " s.";
            case GERMAN:
                return "Zeitüberschreitung beim Generieren der Commit-Nachricht: " + timeoutSeconds + " s.";
            default:
                return "Timed out after " + timeoutSeconds + "s.";
        }
    }

    public static String normalize(String language) {
        for (String value : DISPLAY_NAMES.keySet()) {
            if (value.equals(language)) {
                return value;
            }
        }
        for (Map.Entry<String, String> entry : DISPLAY_NAMES.entrySet()) {
            if (entry.getValue().equals(language)) {
                return entry.getKey();
            }
        }
        return ENGLISH;
    }
}

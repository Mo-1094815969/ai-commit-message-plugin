package com.github.aicommit.action;

import com.github.aicommit.AiCommitBundle;
import com.github.aicommit.commit.CommitMessageCleaner;
import com.github.aicommit.commit.CommitPromptBuilder;
import com.github.aicommit.commit.GitDiffCollector;
import com.github.aicommit.commit.SelectedChangesCollector;
import com.github.aicommit.commit.SensitiveDiffFilter;
import com.github.aicommit.provider.AiProvider;
import com.github.aicommit.provider.AiProviderRegistry;
import com.github.aicommit.provider.EffectiveProviderConfig;
import com.github.aicommit.provider.ProviderConfigResolver;
import com.github.aicommit.settings.AiCommitSettings;
import com.github.aicommit.settings.CommitLanguage;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.ui.AnimatedIcon;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GenerateCommitMessageAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(GenerateCommitMessageAction.class);
    private static final String NOTIFICATION_GROUP = "AI Commit Message Notifications";
    private static final Set<String> IN_FLIGHT_PROJECTS = ConcurrentHashMap.newKeySet();
    private static final Icon ACTION_ICON = IconLoader.getIcon("/icons/ai-commit.svg", GenerateCommitMessageAction.class);
    private static final long STREAM_UPDATE_INTERVAL_MILLIS = 20;

    private final SelectedChangesCollector changesCollector = new SelectedChangesCollector();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        AiCommitSettings.State settings = AiCommitSettings.getInstance().copyState();
        Project project = event.getProject();
        if (project == null) {
            notify(null, CommitLanguage.noProjectMessage(settings.language), NotificationType.WARNING);
            return;
        }
        CommitMessageI commitMessagePanel = changesCollector.getCommitMessagePanel(event);
        if (commitMessagePanel == null) {
            notify(project, CommitLanguage.noPanelMessage(settings.language), NotificationType.WARNING);
            return;
        }
        Collection<Change> changes = changesCollector.getSelectedChanges(event, project);
        if (changes == null || changes.isEmpty()) {
            notify(project, CommitLanguage.noChangesMessage(settings.language), NotificationType.WARNING);
            return;
        }

        String projectKey = project.getBasePath() != null ? project.getBasePath() : project.getName();
        if (!IN_FLIGHT_PROJECTS.add(projectKey)) {
            notify(project, CommitLanguage.alreadyRunningMessage(settings.language), NotificationType.WARNING);
            return;
        }

        Presentation presentation = event.getPresentation();
        commitMessagePanel.setCommitMessage(CommitLanguage.generatingMessage(settings.language));
        presentation.setIcon(AnimatedIcon.Default.INSTANCE);

        CommitMessageCleaner cleaner = new CommitMessageCleaner();
        StringBuilder streamed = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicInteger visualPos = new AtomicInteger(0);
        AtomicReference<Future<?>> workerRef = new AtomicReference<>();
        ScheduledFuture<?> streamTicker = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            if (completed.get()) {
                return;
            }
            String snapshot;
            synchronized (streamed) {
                snapshot = streamed.toString();
            }
            String fullCleaned = cleaner.cleanPartial(snapshot);
            if (fullCleaned.isEmpty()) {
                return;
            }
            int pos = visualPos.get();
            int total = fullCleaned.length();
            if (total <= pos) {
                return;
            }
            int buffered = total - pos;
            int advance = Math.max(1, buffered / 3);
            int newPos = Math.min(total, pos + advance);
            visualPos.set(newPos);
            String toShow = fullCleaned.substring(0, newPos);
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!completed.get()) {
                    commitMessagePanel.setCommitMessage(toShow);
                }
            });
        }, 0, STREAM_UPDATE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> timeout = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (completed.compareAndSet(false, true)) {
                Future<?> worker = workerRef.get();
                if (worker != null) {
                    worker.cancel(true);
                }
                streamTicker.cancel(false);
                IN_FLIGHT_PROJECTS.remove(projectKey);
                ApplicationManager.getApplication().invokeLater(() -> {
                    commitMessagePanel.setCommitMessage("");
                    restoreIcon(presentation);
                    notify(project, CommitLanguage.timeoutMessage(settings.language, settings.timeoutSeconds),
                            NotificationType.ERROR);
                });
            }
        }, settings.timeoutSeconds, TimeUnit.SECONDS);

        Future<?> worker = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                String diff = new GitDiffCollector().collect(changes, new SensitiveDiffFilter(settings));
                if (diff.trim().isEmpty()) {
                    throw new IllegalStateException(CommitLanguage.noDiffMessage(settings.language));
                }
                String prompt = new CommitPromptBuilder().build(diff, settings);
                EffectiveProviderConfig config = new ProviderConfigResolver().resolve(settings);
                AiProvider provider = new AiProviderRegistry().get(config.getKind());
                LOG.info("Generating commit message with provider=" + config.getKind().id()
                        + ", source=" + config.getSource()
                        + ", model=" + safeModel(config.getModel())
                        + ", promptLength=" + prompt.length());
                String raw = provider.generateStreaming(prompt, config, settings.timeoutSeconds, chunk -> {
                    if (completed.get()) {
                        return;
                    }
                    synchronized (streamed) {
                        streamed.append(chunk);
                    }
                });
                String cleaned = cleaner.clean(raw);
                if (cleaned.isEmpty()) {
                    throw new IllegalStateException("AI returned an empty commit message.");
                }
                if (completed.compareAndSet(false, true)) {
                    timeout.cancel(false);
                    streamTicker.cancel(false);
                    IN_FLIGHT_PROJECTS.remove(projectKey);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        commitMessagePanel.setCommitMessage(cleaned);
                        restoreIcon(presentation);
                        notify(project, AiCommitBundle.message("commit.success"), NotificationType.INFORMATION);
                    });
                }
            } catch (Exception e) {
                LOG.warn("AI commit message generation failed: " + e.getMessage(), e);
                if (completed.compareAndSet(false, true)) {
                    timeout.cancel(false);
                    streamTicker.cancel(false);
                    IN_FLIGHT_PROJECTS.remove(projectKey);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                        commitMessagePanel.setCommitMessage("");
                        restoreIcon(presentation);
                        notify(project, CommitLanguage.failureMessage(settings.language, message),
                                NotificationType.ERROR);
                    });
                }
            }
        });
        workerRef.set(worker);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        boolean visible = project != null;
        event.getPresentation().setEnabledAndVisible(visible);
        event.getPresentation().setText(AiCommitBundle.message("action.generate.text"));
        event.getPresentation().setDescription(AiCommitBundle.message("action.generate.description"));
        if (project != null) {
            String projectKey = project.getBasePath() != null ? project.getBasePath() : project.getName();
            if (IN_FLIGHT_PROJECTS.contains(projectKey)) {
                event.getPresentation().setIcon(AnimatedIcon.Default.INSTANCE);
                event.getPresentation().setEnabled(false);
            } else {
                event.getPresentation().setIcon(ACTION_ICON);
            }
        }
    }

    private void restoreIcon(Presentation presentation) {
        presentation.setIcon(ACTION_ICON);
    }

    private void notify(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup(NOTIFICATION_GROUP)
                .createNotification(content, type)
                .notify(project);
    }

    private String safeModel(String model) {
        return model == null || model.trim().isEmpty() ? "default" : model.trim();
    }
}

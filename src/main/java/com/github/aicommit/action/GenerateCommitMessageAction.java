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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class GenerateCommitMessageAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(GenerateCommitMessageAction.class);
    private static final String NOTIFICATION_GROUP = "AI Commit Message Notifications";
    // 按项目保存生成任务，工具栏再次点击时可以定位并终止当前 AI 请求
    private static final ConcurrentMap<String, GenerationTask> IN_FLIGHT_TASKS = new ConcurrentHashMap<>();
    private static final Icon ACTION_ICON = IconLoader.getIcon("/icons/ai-commit.svg", GenerateCommitMessageAction.class);
    private static final Icon LOADING_ICON = AnimatedIcon.Default.INSTANCE;
    private static final Icon STOP_ICON = IconLoader.getIcon("/icons/ai-commit-stop.svg", GenerateCommitMessageAction.class);
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

        String projectKey = projectKey(project);
        GenerationTask runningTask = IN_FLIGHT_TASKS.get(projectKey);
        if (runningTask != null) {
            cancelGeneration(projectKey, runningTask);
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

        Presentation presentation = event.getPresentation();
        GenerationTask task = new GenerationTask(presentation);
        GenerationTask previousTask = IN_FLIGHT_TASKS.putIfAbsent(projectKey, task);
        if (previousTask != null) {
            cancelGeneration(projectKey, previousTask);
            return;
        }

        showRunningIcon(presentation);

        CommitMessageCleaner cleaner = new CommitMessageCleaner();
        StringBuilder streamed = new StringBuilder();
        AtomicInteger visualPos = new AtomicInteger(0);
        ScheduledFuture<?> streamTicker = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            if (task.completed.get()) {
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
                if (!task.completed.get()) {
                    commitMessagePanel.setCommitMessage(toShow);
                }
            });
        }, 0, STREAM_UPDATE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        task.streamTickerRef.set(streamTicker);
        ScheduledFuture<?> timeout = AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (task.completed.compareAndSet(false, true)) {
                Future<?> worker = task.workerRef.get();
                if (worker != null) {
                    worker.cancel(true);
                }
                streamTicker.cancel(false);
                IN_FLIGHT_TASKS.remove(projectKey, task);
                ApplicationManager.getApplication().invokeLater(() -> {
                    commitMessagePanel.setCommitMessage("");
                    restoreIcon(presentation);
                    notify(project, CommitLanguage.timeoutMessage(settings.language, settings.timeoutSeconds),
                            NotificationType.ERROR);
                });
            }
        }, settings.timeoutSeconds, TimeUnit.SECONDS);
        task.timeoutRef.set(timeout);

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
                    if (task.completed.get()) {
                        // 已取消时主动打断 SSE 读取，避免后台继续消费后续 token
                        throw new CancellationException("Commit message generation stopped by user.");
                    }
                    synchronized (streamed) {
                        streamed.append(chunk);
                    }
                });
                String cleaned = cleaner.clean(raw);
                if (cleaned.isEmpty()) {
                    throw new IllegalStateException("AI returned an empty commit message.");
                }
                if (task.completed.compareAndSet(false, true)) {
                    timeout.cancel(false);
                    streamTicker.cancel(false);
                    IN_FLIGHT_TASKS.remove(projectKey, task);
                    ApplicationManager.getApplication().invokeLater(() -> {
                        commitMessagePanel.setCommitMessage(cleaned);
                        restoreIcon(presentation);
                        notify(project, AiCommitBundle.message("commit.success"), NotificationType.INFORMATION);
                    });
                }
            } catch (Exception e) {
                if (task.completed.get()) {
                    LOG.debug("AI commit message generation was cancelled.");
                    return;
                }
                LOG.warn("AI commit message generation failed: " + e.getMessage(), e);
                if (task.completed.compareAndSet(false, true)) {
                    timeout.cancel(false);
                    streamTicker.cancel(false);
                    IN_FLIGHT_TASKS.remove(projectKey, task);
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
        task.workerRef.set(worker);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        boolean visible = project != null;
        event.getPresentation().setEnabledAndVisible(visible);
        event.getPresentation().setText(AiCommitBundle.message("action.generate.text"));
        if (project != null) {
            String projectKey = projectKey(project);
            if (IN_FLIGHT_TASKS.containsKey(projectKey)) {
                showRunningIcon(event.getPresentation());
                event.getPresentation().setEnabled(true);
                event.getPresentation().setDescription(AiCommitBundle.message("action.stop.description"));
            } else {
                restoreIcon(event.getPresentation());
                event.getPresentation().setDescription(AiCommitBundle.message("action.generate.description"));
            }
        }
    }

    private void cancelGeneration(String projectKey, GenerationTask task) {
        // 用户主动停止只结束当前流式输出，不清空或回滚 Commit Message 中已有内容
        if (!task.completed.compareAndSet(false, true)) {
            return;
        }
        Future<?> worker = task.workerRef.get();
        if (worker != null) {
            worker.cancel(true);
        }
        ScheduledFuture<?> timeout = task.timeoutRef.get();
        if (timeout != null) {
            timeout.cancel(false);
        }
        ScheduledFuture<?> streamTicker = task.streamTickerRef.get();
        if (streamTicker != null) {
            streamTicker.cancel(false);
        }
        IN_FLIGHT_TASKS.remove(projectKey, task);
        ApplicationManager.getApplication().invokeLater(() -> restoreIcon(task.presentation));
    }

    private void restoreIcon(Presentation presentation) {
        presentation.setIcon(ACTION_ICON);
        presentation.setHoveredIcon(null);
    }

    private void showRunningIcon(Presentation presentation) {
        // 生成中默认展示 IDE 原生加载动画，悬停时才露出同色系停止入口
        presentation.setIcon(LOADING_ICON);
        presentation.setHoveredIcon(STOP_ICON);
    }

    private String projectKey(Project project) {
        return project.getBasePath() != null ? project.getBasePath() : project.getName();
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

    private static final class GenerationTask {
        private final Presentation presentation;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicReference<Future<?>> workerRef = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> timeoutRef = new AtomicReference<>();
        private final AtomicReference<ScheduledFuture<?>> streamTickerRef = new AtomicReference<>();

        private GenerationTask(Presentation presentation) {
            this.presentation = presentation;
        }
    }
}

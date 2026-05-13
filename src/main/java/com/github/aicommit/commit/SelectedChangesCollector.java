package com.github.aicommit.commit;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.changes.Change;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public final class SelectedChangesCollector {
    private static final Logger LOG = Logger.getInstance(SelectedChangesCollector.class);

    @Nullable
    public CommitMessageI getCommitMessagePanel(@NotNull AnActionEvent event) {
        CommitMessageI messageControl = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        if (messageControl != null) {
            return messageControl;
        }
        Object workflowHandler = getWorkflowHandler(event);
        if (workflowHandler instanceof CommitMessageI) {
            return (CommitMessageI) workflowHandler;
        }
        return null;
    }

    @Nullable
    public Collection<Change> getSelectedChanges(@NotNull AnActionEvent event, @NotNull Project project) {
        Object workflowHandler = getWorkflowHandler(event);
        Collection<Change> included = getIncludedChangesViaReflection(workflowHandler);
        if (included != null) {
            return included;
        }

        Object messageControl = event.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL);
        if (messageControl instanceof CheckinProjectPanel) {
            Collection<Change> selected = ((CheckinProjectPanel) messageControl).getSelectedChanges();
            if (selected != null) {
                return selected;
            }
        }

        Change[] changes = event.getData(VcsDataKeys.CHANGES);
        if (changes != null && changes.length > 0) {
            return Arrays.asList(changes);
        }

        LOG.debug("No selected or included commit changes were available.");
        return null;
    }

    @Nullable
    private Object getWorkflowHandler(AnActionEvent event) {
        try {
            Field field = VcsDataKeys.class.getField("COMMIT_WORKFLOW_HANDLER");
            Object key = field.get(null);
            if (key instanceof DataKey) {
                return event.getData((DataKey<?>) key);
            }
        } catch (NoSuchFieldException e) {
            LOG.debug("COMMIT_WORKFLOW_HANDLER is not available in this IDE version.");
        } catch (Exception e) {
            LOG.debug("Failed to read COMMIT_WORKFLOW_HANDLER: " + e.getMessage());
        }
        return null;
    }

    @Nullable
    private Collection<Change> getIncludedChangesViaReflection(Object workflowHandler) {
        if (workflowHandler == null) {
            return null;
        }
        try {
            Method getUiMethod = workflowHandler.getClass().getMethod("getUi");
            Object ui = getUiMethod.invoke(workflowHandler);
            if (ui == null) {
                return null;
            }
            Method getIncludedChangesMethod = ui.getClass().getMethod("getIncludedChanges");
            Object result = getIncludedChangesMethod.invoke(ui);
            if (!(result instanceof Collection)) {
                return null;
            }
            Collection<?> raw = (Collection<?>) result;
            Collection<Change> changes = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof Change) {
                    changes.add((Change) item);
                }
            }
            return changes;
        } catch (Exception e) {
            LOG.debug("Failed to get included changes via reflection: " + e.getMessage());
            return null;
        }
    }
}

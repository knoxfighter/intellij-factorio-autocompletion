package moe.knox.factorio.core;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.intellij.FactorioConfig;
import org.jetbrains.annotations.NotNull;

@Service
final public class NotificationService {
    private final NotificationGroup notificationGroup;
    private final Project project;

    public NotificationService(Project project) {
        this.project = project;
        this.notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("Factorio LUA API Support");
    }

    public static NotificationService getInstance(Project project) {
        return project.getService(NotificationService.class);
    }

    public void notifyErrorCheckingNewVersion() {
        raiseNotification("Error checking new Version. Manual update in the Settings.", NotificationType.WARNING);
    }

    public void notifyErrorCreatingApiDirs() {
        raiseNotification("Error creating the directories for the Factorio API.", NotificationType.ERROR);
    }

    public void notifyErrorCreatingPrototypeDirs() {
        raiseNotification("Error creating the directories for the Factorio Prototypes.", NotificationType.ERROR);
    }

    public void notifyErrorDownloadingVersion() {
        raiseNotification("Error downloading Version overview", NotificationType.WARNING);
    }

    public void notifyErrorDownloadingApi() {
        raiseNotification("Error downloading api", NotificationType.ERROR);
    }

    public void notifyErrorDownloadingPrototypeDefinitions() {
        raiseNotification("""
                    Error downloading the factorio prototype definitions. Please go online and try it again!
                    Integration is disabled until reloaded in Settings.
                """, NotificationType.ERROR);
    }

    public void notifyErrorDownloadingPartPrototypeDefinitions() {
        raiseNotification("""
                    Error downloading parts of the factorio prototype definitions. Please try again later!
                    Integration is partially disabled until reloaded in Settings.
                """, NotificationType.ERROR);
    }

    public void notifyErrorTagsDownloading() {
        raiseNotification("Error getting current tags. Lualib not downloaded.", NotificationType.WARNING);
    }

    public void notifyErrorCreatingLuaLib() {
        raiseNotification("Error creating LuaLib directory", NotificationType.WARNING);
    }

    private void raiseNotification(String message, NotificationType notificationType) {
        Notification notification = notificationGroup.createNotification(message, notificationType);
        notification.addAction(createOpenSettingsNotificationAction());
        Notifications.Bus.notify(notification, project);
    }

    private NotificationAction createOpenSettingsNotificationAction() {
        return new NotificationAction("Open Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioConfig.class);
            }
        };
    }
}

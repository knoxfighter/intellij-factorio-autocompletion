package moe.knox.factorio.core;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.intellij.FactorioAutocompletionConfig;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Service
final public class NotificationService {
    private final NotificationGroup notificationGroup;
    private final Project project;

    public NotificationService(Project project) {
        this.project = project;
        this.notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("default");
    }

    public static NotificationService getInstance(Project project) {
        return project.getService(NotificationService.class);
    }

    public void notifyErrorCheckingNewVersion() {
        raiseNotification(Message.ERROR_CHECK_VERSION, NotificationType.WARNING);
    }

    public void notifyErrorCreatingApiDirs() {
        raiseNotification(Message.ERROR_CREATE_API_DIR, NotificationType.ERROR);
    }

    public void notifyErrorCreatingPrototypeDirs() {
        raiseNotification(Message.ERROR_CREATE_PROTOTYPE_DIR, NotificationType.ERROR);
    }

    public void notifyErrorDownloadingVersion() {
        raiseNotification(Message.ERROR_DOWNLOAD_VERSION, NotificationType.WARNING);
    }

    public void notifyErrorDownloadingPrototypeDefinitions() {
        raiseNotification(Message.ERROR_DOWNLOAD_PROTOTYPE_DEFINITIONS, NotificationType.ERROR);
    }

    public void notifyErrorDownloadingPartPrototypeDefinitions() {
        raiseNotification(Message.ERROR_DOWNLOAD_PARTS_PROTOTYPE_DEFINITIONS, NotificationType.ERROR);
    }

    public void notifyErrorTagsDownloading() {
        raiseNotification(Message.ERROR_GETTING_CURRENT_TAGS, NotificationType.WARNING);
    }

    private void raiseNotification(Message message, NotificationType notificationType) {
        Notification notification = notificationGroup.createNotification(message.message, notificationType);
        notification.addAction(createOpenSettingsNotificationAction());
        Notifications.Bus.notify(notification, project);
    }

    private NotificationAction createOpenSettingsNotificationAction() {
        return new NotificationAction("Open Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
            }
        };
    }

    private enum Message
    {
        ERROR_GETTING_CURRENT_TAGS("Error getting current tags. Lualib not downloaded."),
        ERROR_DOWNLOAD_VERSION("Error downloading Version overview"),
        ERROR_CHECK_VERSION("Error checking new Version. Manual update in the Settings."),
        ERROR_CREATE_API_DIR("Error creating the directories for the Factorio API."),
        ERROR_CREATE_PROTOTYPE_DIR("Error creating the directories for the Factorio Prototypes."),
        ERROR_DOWNLOAD_PROTOTYPE_DEFINITIONS("""
            Error downloading the factorio prototype definitions. Please go online and try it again!
            Integration is disabled until reloaded in Settings.
        """),
        ERROR_DOWNLOAD_PARTS_PROTOTYPE_DEFINITIONS("""
            Error downloading parts of the factorio prototype definitions. Please try again later!
            Integration is partially disabled until reloaded in Settings.
        """);

        private final String message;

        Message(String message) {
            this.message = message;
        }
    }
}

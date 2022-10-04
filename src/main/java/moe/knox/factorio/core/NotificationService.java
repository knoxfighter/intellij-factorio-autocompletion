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

    public void notifyErrorLuaLibUpdating() {
        raiseNotification("""
            Error checking LuaLib update. Please try again later!
        """, NotificationType.ERROR);
    }

    public void notifyErrorPrototypeUpdating() {
        raiseNotification("""
            Error checking Prototype update. Please try again later!
        """, NotificationType.ERROR);
    }

    public void notifyErrorApiUpdating() {
        raiseNotification("""
            Error checking Factorio Api update. Please try again later!
        """, NotificationType.ERROR);
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

package moe.knox.factorio.parser;

import com.google.common.io.Files;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import moe.knox.factorio.FactorioAutocompletionConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public abstract class FactorioParser extends Task.Backgroundable {
    protected static NotificationGroup notificationGroup = new NotificationGroup("Factorio API Download", NotificationDisplayType.STICKY_BALLOON, true);
    protected static String newLine = System.lineSeparator();

    public FactorioParser(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title) {
        super(project, title);
    }

    public FactorioParser(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    public FactorioParser(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled, @Nullable PerformInBackgroundOption backgroundOption) {
        super(project, title, canBeCancelled, backgroundOption);
    }

    /**
     * Shows error in the balloon "Event Log" of the IDE. This is useful, to inform the user, that the download failed.
     * Not all download fails report in an unusable autocompletion, some results in only partially unavailable autocompletion.
     *
     * @param isPart If the error only affects part of the autocompletion.
     */
    protected void showDownloadingError(boolean isPart) {
        String downloadingError;
        if (!isPart) {
            downloadingError = "Error downloading the factorio prototype definitions. Please go online and try it again!" + newLine +
                    "Integration is disabled until reloaded in Settings.";
        } else {
            downloadingError = "Error downloading parts of the factorio prototype definitions. Please try again later!" + newLine +
                    "Integration is partially disabled until reloaded in Settings.";
        }
        Notification notification = notificationGroup.createNotification(downloadingError, NotificationType.ERROR);
        notification.addAction(new NotificationAction("Open Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
            }
        });
        Notifications.Bus.notify(notification, myProject);
    }

    /**
     * removed "::" and all variants of newLines from the given string and returns it.
     *
     * @param s remove from this String
     * @return string with removed things
     */
    @NotNull
    @Contract(pure = true)
    protected String removeNewLines(@NotNull String s) {
        return s.replaceAll("(::)|(\\r\\n|\\r|\\n)", "");
    }

    /**
     * Save the fileContent to the specified file. The file is saved within the main application and with write access.
     * The result of this invoke of the main application is avaited.
     *
     * @param filePath    The Path to the file to save to
     * @param fileContent The content of the file
     */
    protected void saveStringToFile(String filePath, String fileContent) {
        // create file
        File file = new File(filePath);
        try {
            file.createNewFile();
            Files.write(fileContent.getBytes(), file);
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }
    }
}

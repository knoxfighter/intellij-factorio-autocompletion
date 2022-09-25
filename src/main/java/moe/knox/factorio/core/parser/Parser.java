package moe.knox.factorio.core.parser;

import com.google.common.io.Files;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import moe.knox.factorio.core.NotificationService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public abstract class Parser extends Task.Backgroundable {
    protected static String newLine = System.lineSeparator();

    public Parser(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
    }

    /**
     * Shows error in the balloon "Event Log" of the IDE. This is useful, to inform the user, that the download failed.
     * Not all download fails report in an unusable autocompletion, some results in only partially unavailable autocompletion.
     *
     * @param isPart If the error only affects part of the autocompletion.
     */
    protected void showDownloadingError(boolean isPart) {
        var notificationService = NotificationService.getInstance(myProject);

        if (isPart) {
            notificationService.notifyErrorDownloadingPartPrototypeDefinitions();
        } else {
            notificationService.notifyErrorDownloadingPrototypeDefinitions();
        }
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

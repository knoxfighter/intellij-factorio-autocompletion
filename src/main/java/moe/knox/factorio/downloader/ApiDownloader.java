package moe.knox.factorio.downloader;

import com.google.gson.Gson;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.FactorioAutocompletionConfig;
import moe.knox.factorio.library.FactorioLibraryProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static moe.knox.factorio.downloader.DownloaderContainer.apiRootPath;
import static moe.knox.factorio.downloader.DownloaderContainer.downloadApiLink;

class ApiDownloader extends Task.Backgroundable implements Downloader {
    protected static NotificationGroup notificationGroup = new NotificationGroup("Factorio API Download", NotificationDisplayType.STICKY_BALLOON, true);
    private AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private ProgressIndicator indicator;
    private String version;

    ApiDownloader(@Nullable Project project, @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title) {
        super(project, title, true);
    }

    @Override
    public void onCancel() {
        super.onCancel();
        printMessage("Cancelled downloading the factorio LUA-API files. They are know only partially available.\n" +
                "Please reload the integration in Settings.");
    }

    @Override
    public synchronized boolean download(String version) {
        // dont do anything if download in progress
        if (downloadInProgress.compareAndSet(false, true)) {
            this.version = version;
            ProgressManager.getInstance().run(this);
            return true;
        } else {
            return false;
        }
    }

    public String getCurrent(String version) {
        if (downloadInProgress.get() || (this.indicator != null && (this.indicator.isCanceled() || this.indicator.isRunning()))) {
            return null;
        }
        return apiRootPath + version;
    }

    @Override
    public synchronized void cancel() {
        if (downloadInProgress.get() && !this.indicator.isCanceled() && this.indicator.isRunning()) {
            this.indicator.cancel();
        }
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        this.indicator = indicator;

        // check if already downloaded
        String apiPath = apiRootPath + version + "/";
        if (!FileUtil.exists(apiPath)) {
            // create directories
            if (FileUtil.createDirectory(new File(apiPath))) {
                // download API
                try {
                    // download the list page
                    String allFilesLink = downloadApiLink + version + "/";
                    URL allFilesURL = new URL(allFilesLink);
                    InputStream allFilesStream = allFilesURL.openStream();
                    indicator.setIndeterminate(false);

                    // parse the list page
                    String[] allFiles = new Gson().fromJson(new InputStreamReader(allFilesStream), String[].class);

                    // iterate over all listed files, download them and save them
                    for (int i = 0, allFilesLength = allFiles.length; i < allFilesLength; i++) {
                        String file = allFiles[i];
                        if (indicator.isCanceled()) {
                            break;
                        }
                        indicator.setFraction((double) i / allFilesLength);
                        String singleFileLink = allFilesLink + file;
                        URL singleFileURL = new URL(singleFileLink);
                        InputStream singleFileStream = singleFileURL.openStream();
                        Files.copy(singleFileStream, Paths.get(apiPath + file));
                        singleFileStream.close();
                    }
                } catch (IOException e) {
                    if (!indicator.isCanceled()) {
                        e.printStackTrace();
                        printMessage("Error downloading the factorio LUA-API files. Please go online and try it again!\n" +
                                "Integration is disabled until reloaded in Settings.");
                    }
                }

                ApplicationManager.getApplication().invokeLater(() ->
                        FactorioLibraryProvider.reload()
                );
            }
        }
        downloadInProgress.set(false);
    }

    private void printMessage(String message) {
        Notification notification = notificationGroup.createNotification(
                message,
                NotificationType.ERROR
        );
        notification.addAction(new NotificationAction("Open settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
            }
        });
        Notifications.Bus.notify(notification, myProject);
    }
}

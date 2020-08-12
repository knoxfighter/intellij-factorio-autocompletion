package moe.knox.factorio.parser;

import com.google.gson.Gson;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.FactorioAutocompletionConfig;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.library.FactorioLibraryProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioApiParser extends FactorioParser {
    public static String apiRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_api/";
    public static String factorioApiBaseLink = "https://lua-api.factorio.com/";
    public static String factorioParsedApiLink = "http://172.17.0.2:8080/api/";

    private static NotificationGroup notificationGroup = new NotificationGroup("Factorio API Download", NotificationDisplayType.STICKY_BALLOON, true);
    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;

    public FactorioApiParser(@Nullable Project project, String saveDir, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        this.saveDir = saveDir;
    }

    /**
     * get the current API Link. If the API is not there, the download will be started in a background thread.
     * When the download is in progress, this function returns instantly null.
     *
     * @param project
     * @return the path to the API or null
     */
    @Nullable
    synchronized public static String getCurrentApiLink(Project project) {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = apiRootPath + config.selectedFactorioVersion.link;

        // check if API is downloaded
        File apiPathFile = new File(apiPath);
        if (apiPathFile.exists()) {
            return apiPath;
        } else {
            // request download API
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioApiParser(project, apiPath, "Download and Parse Factorio API", false));
            }
            return null;
        }
    }

    public static void removeCurrentAPI(Project project) {
        if (!downloadInProgress.get()) {
            FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
            String apiPath = apiRootPath + config.selectedFactorioVersion.link;
            FileUtil.delete(new File(apiPath));
            FactorioLibraryProvider.reload();
        }
    }

    public static void checkForUpdate(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = apiRootPath + config.selectedFactorioVersion.link;

        if (config.selectedFactorioVersion.desc.equals("Latest version")) {
            Document doc = null;
            try {
                doc = Jsoup.connect("https://lua-api.factorio.com/").get();
            } catch (IOException e) {
//                    e.printStackTrace();
                Notification notification = notificationGroup.createNotification("Error checking new Version. Manual update in the Settings.", NotificationType.WARNING);
                notification.addAction(new NotificationAction("Open Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                    }
                });
            }
            if (!doc.select("a").get(1).text().equals(config.curVersion)) {
                // new version detected, update it
                removeCurrentAPI(project);
                if (downloadInProgress.compareAndSet(false, true)) {
                    ProgressManager.getInstance().run(new FactorioApiParser(project, apiPath, "Download and Parse Factorio API", false));
                }
            }
        }
    }

    /**
     * Entry-point for the Task.Backgroundable.
     * This is the basic entrypoint for the ProgressManager started Thread.
     * After the download is finished, the FactorioLibraryProvider is reloaded.
     *
     * @param indicator
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        this.indicator = indicator;
        config = FactorioAutocompletionState.getInstance(myProject);

        // start the whole thing
        assureDir();

        downloadInProgress.set(false);

        // whole thing finished, reload the Library-Provider
        ApplicationManager.getApplication().invokeLater(() -> FactorioLibraryProvider.reload());
    }

    /**
     * Entry-point with creating the used directory to the save the directory to.
     * It will assure, that the directory is there and will start the download and parsing.
     */
    private void assureDir() {
        File dirFile = new File(saveDir);
        if (!dirFile.exists()) {
            // file does not exist ... create it
            if (dirFile.mkdirs()) {
                // download and parse API
                downloadAPI();
            } else {
                Notification notification = notificationGroup.createNotification("Error creating the directories for the Factorio API.", NotificationType.ERROR);
                notification.addAction(new NotificationAction("Open Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
                    }
                });
                Notifications.Bus.notify(notification, myProject);
            }
        }
    }

    /**
     * Entry-point for the downloading.
     * Download the overview page, where all files are listed. Then download all the listed files.
     */
    private void downloadAPI() {
        String allFilesLink = factorioParsedApiLink + config.selectedFactorioVersion.link;
        InputStream allFilesStream;
        try {
            // download the list page
            URL allFilesURL = new URL(allFilesLink);
            allFilesStream = allFilesURL.openStream();
            indicator.setIndeterminate(false);

            // parse the list page
            String[] allFiles = new Gson().fromJson(new InputStreamReader(allFilesStream), String[].class);

            // iterate over all listed files, download them and save them
            for (int i = 0, allFilesLength = allFiles.length; i < allFilesLength; i++) {
                String file = allFiles[i];
                indicator.setFraction(i / allFilesLength);
                String singleFileLink = allFilesLink + file;
                URL singleFileURL = new URL(singleFileLink);
                InputStream singleFileStream = singleFileURL.openStream();
                Files.copy(singleFileStream, Paths.get(saveDir + file));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }
    }
}

package moe.knox.factorio.core.parser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.intellij.FactorioAutocompletionState;
import moe.knox.factorio.core.NotificationService;
import moe.knox.factorio.core.version.ApiVersionCollection;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.intellij.FactorioAutocompletionState;
import moe.knox.factorio.intellij.FactorioLibraryProvider;
import moe.knox.factorio.core.parser.apiData.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiParser extends Parser {
    public static String apiRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_api/";
    public static String factorioApiBaseLink = "https://lua-api.factorio.com/";
    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;
    private double curTodo = 0;
    private double maxTodo = 0;

    private Writer writer = new Writer();

    public ApiParser(@Nullable Project project, String saveDir, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
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

        String apiPath = getSelectedApiVersionFilePath(project);

        // check if API is downloaded
        File apiPathFile = new File(apiPath);
        if (apiPathFile.exists()) {
            return apiPath;
        } else {
            // request download API
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new ApiParser(project, apiPath, "Download and Parse Factorio API", false));
            }
            return null;
        }
    }

    public static void removeCurrentAPI(Project project) {
        if (!downloadInProgress.get()) {
            String apiPath = getSelectedApiVersionFilePath(project);
            FileUtil.delete(new File(apiPath));
            FactorioLibraryProvider.reload();
        }
    }

    public static void checkForUpdate(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = getSelectedApiVersionFilePath(project);

        if (config.useLatestVersion) {
            var newestVersion = detectLatestAllowedVersion(project);

            if (newestVersion != null && !newestVersion.equals(config.selectedFactorioVersion)) {
                // new version detected, update it
                removeCurrentAPI(project);
                if (downloadInProgress.compareAndSet(false, true)) {
                    ProgressManager.getInstance().run(new ApiParser(project, apiPath, "Download and Parse Factorio API", false));
                }
            }
        }
    }

    private static FactorioApiVersion detectLatestAllowedVersion(Project project)
    {
        ApiVersionCollection factorioApiVersions;

        try {
            factorioApiVersions = (new ApiVersionResolver()).supportedVersions();
        } catch (IOException e) {
            NotificationService.getInstance(project).notifyErrorCheckingNewVersion();
            return null;
        }

        return factorioApiVersions.latestVersion();
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
        try {
            this.indicator = indicator;
            config = FactorioAutocompletionState.getInstance(myProject);

            // start the whole thing
            assureDir();

            // whole thing finished, reload the Library-Provider
            ApplicationManager.getApplication().invokeLater(() -> FactorioLibraryProvider.reload());
        }
        finally {
            downloadInProgress.set(false);
            indicator.stop();
        }
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
                downloadAndParseAPI();
            } else {
                NotificationService.getInstance(myProject).notifyErrorCreatingApiDirs();
            }
        }
    }

    private void updateIndicator() {
        indicator.setFraction(curTodo / maxTodo);
        curTodo++;
    }

    /**
     * Entry-point for the whole parsing.
     * Download the main-Page of the factorio-lua-api, parse all classes and start the other parsers.
     * Here also the indicator will be updated, to show the current percentage of the parsing.
     */
    private void downloadAndParseAPI() {
        String versionedApiLink = factorioApiBaseLink + config.selectedFactorioVersion.version() + "/runtime-api.json";

        JsonAPI jsonAPI;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new URL(versionedApiLink).openStream());
            jsonAPI = JsonAPI.read(inputStreamReader);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        config.curVersion = jsonAPI.application_version;

        String saveFile = saveDir + "factorio.lua";
        // create file

        OutputStreamWriter output;
        try {
            File file = new File(saveFile);
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file, false);
            output = new OutputStreamWriter(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }

        try {
            // builtin types done in `resources/library/builtin-types.lua`

            writer.writeGlobalsObjects(output, jsonAPI.globalObjects);

            output.append("---@class defines").append(newLine);
            output.append("defines = {}").append(newLine).append(newLine);
            writer.writeDefines(output, jsonAPI.defines, "defines");

            // TODO: implement autocompletion for events

            writer.writeClasses(output, jsonAPI.classes);

            writer.writeConcepts(output, jsonAPI.concepts);

            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }
    }

    private static String getSelectedApiVersionFilePath(Project project)
    {
        var config = FactorioAutocompletionState.getInstance(project);

        return apiRootPath + config.selectedFactorioVersion.version();
    }
}

package moe.knox.factorio.core.parser.api;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.core.parser.api.writer.Writer;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.core.NotificationService;
import moe.knox.factorio.core.parser.Parser;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.core.version.ApiVersionCollection;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.intellij.FactorioLibraryProvider;
import moe.knox.factorio.core.parser.api.data.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiParser extends Parser {
    private final static String apiRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_api/";
    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private FactorioState config;
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

        Path apiPath = getApiRuntimeDir(project);

        // check if API is downloaded
        if (Files.exists(apiPath)) {
            return apiPath.toString();
        } else {
            // request download API
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new ApiParser(project, apiPath.toString(), "Download and Parse Factorio API", false));
            }
            return null;
        }
    }

    public static void removeCurrentAPI(Project project) {
        if (!downloadInProgress.get()) {
            Path apiPath = getApiRuntimeDir(project);
            FileUtil.delete(apiPath.toFile());
            FactorioLibraryProvider.reload();
        }
    }

    public static void checkForUpdate(Project project) {
        FactorioState config = FactorioState.getInstance(project);

        if (config.useLatestVersion) {
            var newestVersion = detectLatestAllowedVersion(project);

            if (newestVersion != null && !newestVersion.equals(config.selectedFactorioVersion)) {
                // new version detected, update it
                removeCurrentAPI(project);
                if (downloadInProgress.compareAndSet(false, true)) {
                    Path apiPath = getApiRuntimeDir(project);
                    ProgressManager.getInstance().run(new ApiParser(project, apiPath.toString(), "Download and Parse Factorio API", false));
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
            config = FactorioState.getInstance(myProject);

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
        ApiSpecificationParser apiSpecificationParser = new ApiSpecificationParser();

        RuntimeApi runtimeApi;

        runtimeApi = apiSpecificationParser.parse(config.selectedFactorioVersion);

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

            writer.writeGlobalsObjects(output, runtimeApi.globalObjects);

            output.append("---@class defines").append(newLine);
            output.append("defines = {}").append(newLine).append(newLine);
            writer.writeDefines(output, runtimeApi.defines, "defines");

            // TODO: implement autocompletion for events

            writer.writeClasses(output, runtimeApi.classes);

            writer.writeConcepts(output, runtimeApi.concepts);

            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }
    }

    private static Path getApiRuntimeDir(Project project)
    {
        var config = FactorioState.getInstance(project);

        return Paths.get(apiRootPath, config.selectedFactorioVersion.version());
    }
}

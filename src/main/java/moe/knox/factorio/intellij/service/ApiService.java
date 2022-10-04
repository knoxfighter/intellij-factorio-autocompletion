package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.intellij.NotificationService;
import moe.knox.factorio.core.parser.api.ApiParser;
import moe.knox.factorio.core.version.FactorioVersionCollection;
import moe.knox.factorio.core.version.FactorioVersionResolver;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.intellij.FactorioLibraryProvider;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class ApiService {
    private static final Logger LOG = Logger.getInstance(ApiService.class);
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private final Project project;
    private final ApiParser apiParser;

    private ApiService(Project project) {
        this.project = project;

        Path pluginDir = FileUtil.getPluginDir();
        Path apiRootPath = pluginDir.resolve("factorio_api");
        apiParser = new ApiParser(apiRootPath);
    }

    public static ApiService getInstance(Project project)
    {
        return new ApiService(project);
    }

    public void removeCurrentAPI() {
        if (downloadInProgress.get()) {
            return;
        }

        apiParser.removeCurrentAPI();
        FactorioLibraryProvider.reload();
    }

    public void checkForUpdate() {
        FactorioState config = FactorioState.getInstance(project);

        if (!config.useLatestVersion) {
            return;
        }

        FactorioVersion newestVersion = detectLatestAllowedVersion();

        if (newestVersion != null && !newestVersion.equals(config.selectedFactorioVersion)) {
            removeCurrentAPI();

            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new ApiTask());
            }
        }
    }

    public Path getApiPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = apiParser.getApiPath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new ApiTask());
        }

        return path;
    }

    private FactorioVersion detectLatestAllowedVersion() {
        FactorioVersionCollection factorioApiVersions;

        try {
            factorioApiVersions = (new FactorioVersionResolver()).supportedVersions();
        } catch (IOException e) {
            LOG.error(e);
            NotificationService.getInstance(project).notifyErrorApiUpdating();
            return null;
        }

        return factorioApiVersions.latestVersion();
    }

    private class ApiTask extends Task.Backgroundable {
        public ApiTask() {
            super(project, "Download and Parse Factorio API", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                apiParser.parse(selectedVersion);

                ApplicationManager.getApplication().invokeLater(FactorioLibraryProvider::reload);
            } catch (Throwable e) {
                LOG.error(e);
                NotificationService.getInstance(project).notifyErrorApiUpdating();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}

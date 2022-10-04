package moe.knox.factorio.intellij.library.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.core.GettingTagException;
import moe.knox.factorio.core.LuaLibDownloader;
import moe.knox.factorio.core.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class LuaLibService {
    private static final Logger LOG = Logger.getInstance(LuaLibService.class);
    private final LuaLibDownloader luaLibDownloader;
    private final Project project;
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private LuaLibService(Project project) {
        this.project = project;

        Path pluginDir = FilesystemUtil.getPluginDir();
        Path luaLibRootPath = pluginDir.resolve("lualib");
        Path corePrototypesRootPath = pluginDir.resolve("core_prototypes");
        luaLibDownloader = new LuaLibDownloader(luaLibRootPath, corePrototypesRootPath);
    }

    public static LuaLibService getInstance(Project project)
    {
        return new LuaLibService(project);
    }

    public Path getCurrentLuaLibPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = luaLibDownloader.getLuaLibPath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new LuaLibDownloadTask());
        }

        return path;
    }

    public Path getCurrentCorePrototypePath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = luaLibDownloader.getPrototypePath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new LuaLibDownloadTask());
        }

        return path;
    }

    public void removeLuaLibFiles() {
        if (downloadInProgress.get()) {
            return;
        }

        luaLibDownloader.removeLuaLibFiles();
        PrototypesService.getInstance(project).reloadIndex();
    }

    public boolean checkForUpdate() {
        boolean needUpdate = false;

        try {
            FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

            needUpdate = luaLibDownloader.checkForUpdate(selectedVersion);

            if (needUpdate && downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new LuaLibDownloadTask());
            }
        } catch (GettingTagException e) {
            LOG.error(e);
            NotificationService.getInstance(project).notifyErrorTagsDownloading();
        }

        return needUpdate;
    }

    private class LuaLibDownloadTask extends Task.Backgroundable {
        public LuaLibDownloadTask() {
            super(project, "Download Factorio Lualib", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                luaLibDownloader.downloadAll(selectedVersion);

                ApplicationManager.getApplication().invokeLater(() -> PrototypesService.getInstance(project).reloadIndex());
            } catch (GettingTagException e) {
                LOG.error(e);
                NotificationService.getInstance(project).notifyErrorTagsDownloading();
            } catch (IOException e) {
                LOG.error(e);
                NotificationService.getInstance(project).notifyErrorCreatingLuaLib();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}

package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.core.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.parser.prototype.PrototypeParser;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.intellij.FactorioLibraryProvider;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrototypeService {
    private static final Logger LOG = Logger.getInstance(PrototypeService.class);
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private final Project project;
    private final PrototypeParser prototypeParser;

    private PrototypeService(Project project) {
        this.project = project;

        Path pluginDir = FilesystemUtil.getPluginDir();
        Path prototypesRootPath = pluginDir.resolve("factorio_prototypes");
        prototypeParser = new PrototypeParser(prototypesRootPath);
    }

    public static PrototypeService getInstance(Project project)
    {
        return new PrototypeService(project);
    }


    /**
     * @return return path only if it not empty
     */
    public Path getPrototypePath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = prototypeParser.getPrototypePath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new PrototypeService.PrototypeTask());
        }

        return path;
    }

    public void removeCurrentPrototypes() {
        if (downloadInProgress.get()) {
            return;
        }

        prototypeParser.removeFiles();
        PrototypesService.getInstance(project).reloadIndex();
    }

    public void checkForUpdate() {
        FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = prototypeParser.getPrototypePath(selectedVersion);

        if (path == null) {
            ProgressManager.getInstance().run(new PrototypeService.PrototypeTask());
        }
    }

    public List<String> parsePrototypeTypes() throws IOException {
        return prototypeParser.parsePrototypeTypes();
    }

    private class PrototypeTask extends Task.Backgroundable {
        public PrototypeTask() {
            super(project, "Download and Parse Factorio Prototypes", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                prototypeParser.parse(selectedVersion);

                ApplicationManager.getApplication().invokeLater(FactorioLibraryProvider::reload);
            } catch (Throwable e) {
                LOG.error(e);
                NotificationService.getInstance(project).notifyErrorPrototypeUpdating();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}

package moe.knox.factorio.intellij.library.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.CustomLog;
import moe.knox.factorio.core.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.parser.prototype.PrototypeParser;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.intellij.FactorioLibraryProvider;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class PrototypeService {
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
    public Optional<Path> getPrototypePath() {
        if (downloadInProgress.get()) {
            return Optional.empty();
        }

        FactorioApiVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = prototypeParser.getPrototypePath(version);

        if (path.isEmpty() && downloadInProgress.compareAndSet(false, true)) {
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
        FactorioApiVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

        Optional<Path> path = prototypeParser.getPrototypePath(selectedVersion);

        if (path.isEmpty()) {
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
                FactorioApiVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                prototypeParser.parse(selectedVersion);

                ApplicationManager.getApplication().invokeLater(FactorioLibraryProvider::reload);
            } catch (IOException e) {
                log.error(e);
                NotificationService.getInstance(project).notifyErrorDownloadingPrototypeDefinitions();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}

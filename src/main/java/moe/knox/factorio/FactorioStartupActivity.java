package moe.knox.factorio;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import moe.knox.factorio.downloader.DownloaderContainer;
import moe.knox.factorio.indexer.BasePrototypesService;
import moe.knox.factorio.prototypeDefinition.PrototypeDefinitionService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class FactorioStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);

        if (config.integrationActive) {
            try {
                FactorioAutocompletionState.getInstance(project).reloadAvailableVersions();
            } catch (IOException e) {
                e.printStackTrace();
            }

            DownloaderContainer downloader = DownloaderContainer.getInstance(project);
            downloader.updateIfNeeded();

            // reload core/base prototypes
            BasePrototypesService.getInstance(project).reloadIndex();

            // reload prototype definitions
            PrototypeDefinitionService.getInstance(project).reload();
        }
    }
}

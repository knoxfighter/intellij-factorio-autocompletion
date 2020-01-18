package moe.knox.factorio;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import moe.knox.factorio.indexer.BasePrototypesService;
import moe.knox.factorio.parser.FactorioApiParser;
import moe.knox.factorio.parser.FactorioLualibParser;
import moe.knox.factorio.parser.FactorioPrototypeParser;
import org.jetbrains.annotations.NotNull;

public class FactorioStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);

        if (config.integrationActive) {
            boolean update = FactorioLualibParser.checkForUpdate(project);
            FactorioApiParser.checkForUpdate(project);

            if (update) {
                // reload Prototypes
                FactorioPrototypeParser.removeCurrentPrototypes();
                FactorioPrototypeParser.getCurrentPrototypeLink(project);
            }

            // reload core/base prototypes
            BasePrototypesService.getInstance(project).reloadIndex();
        }
    }
}

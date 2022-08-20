package moe.knox.factorio.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import moe.knox.factorio.core.BasePrototypesService;
import moe.knox.factorio.core.parser.ApiParser;
import moe.knox.factorio.core.parser.LuaLibParser;
import moe.knox.factorio.core.parser.PrototypeParser;
import org.jetbrains.annotations.NotNull;

public class FactorioStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);

        if (config.integrationActive) {
            boolean update = LuaLibParser.checkForUpdate(project);
            ApiParser.checkForUpdate(project);

            if (update) {
                // reload Prototypes
                PrototypeParser.removeCurrentPrototypes();
                PrototypeParser.getCurrentPrototypeLink(project);
            }

            // reload core/base prototypes
            BasePrototypesService.getInstance(project).reloadIndex();
        }
    }
}

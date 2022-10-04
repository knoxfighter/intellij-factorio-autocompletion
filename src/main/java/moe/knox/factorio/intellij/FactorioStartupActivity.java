package moe.knox.factorio.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.parser.api.ApiParser;
import moe.knox.factorio.intellij.library.service.LuaLibService;
import moe.knox.factorio.intellij.library.service.PrototypeService;
import org.jetbrains.annotations.NotNull;

public class FactorioStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        FactorioState config = FactorioState.getInstance(project);

        if (config.integrationActive) {
            boolean update = LuaLibService.getInstance(project).checkForUpdate();
            ApiParser.checkForUpdate(project);

            if (update) {
                PrototypeService.getInstance(project).removeCurrentPrototypes();
                PrototypeService.getInstance(project).checkForUpdate();
            }

            // reload core/base prototypes
            PrototypesService.getInstance(project).reloadIndex();
        }
    }
}

package moe.knox.factorio.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.intellij.service.ApiService;
import moe.knox.factorio.intellij.service.LuaLibService;
import moe.knox.factorio.intellij.service.PrototypeService;
import org.jetbrains.annotations.NotNull;

public class FactorioStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        FactorioState config = FactorioState.getInstance(project);

        if (config.integrationActive) {
            boolean update = LuaLibService.getInstance(project).checkForUpdate();
            ApiService.getInstance(project).checkForUpdate();

            if (update) {
                PrototypeService.getInstance(project).removeLibraryFiles();
                PrototypeService.getInstance(project).checkForUpdate();
            }

            // reload core/base prototypes
            PrototypesService.getInstance(project).reloadIndex();
        }
    }
}

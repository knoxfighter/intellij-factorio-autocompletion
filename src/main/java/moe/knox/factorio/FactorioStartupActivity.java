package moe.knox.factorio;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import moe.knox.factorio.library.FactorioApiParser;
import moe.knox.factorio.library.FactorioLualibParser;
import moe.knox.factorio.library.FactorioPrototypeParser;
import org.jetbrains.annotations.NotNull;

public class FactorioStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        boolean update = FactorioLualibParser.checkForUpdate(project);
        FactorioApiParser.checkForUpdate(project);

        if (update) {
            // reload Prototypes
            FactorioPrototypeParser.removeCurrentPrototypes();
            FactorioPrototypeParser.getCurrentPrototypeLink(project);
        }
    }
}

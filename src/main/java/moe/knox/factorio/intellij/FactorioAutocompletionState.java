package moe.knox.factorio.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@State(
        name = "FactorioAutocompletionConfig",
        storages = {
                @Storage("FactorioAutocompletionConfig.xml")
        }
)
public class FactorioAutocompletionState implements PersistentStateComponent<FactorioAutocompletionState> {
    public boolean integrationActive = false;
    public String curVersion = "";
    @NotNull
    public FactorioApiVersion selectedFactorioVersion;
    public String currentLualibVersion = "";
    public boolean useLatestVersion = true;

    public FactorioAutocompletionState() throws IOException {
        selectedFactorioVersion = (new ApiVersionResolver()).supportedVersions().latestVersion();
    }

    @Nullable
    @Override
    public FactorioAutocompletionState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FactorioAutocompletionState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    public static FactorioAutocompletionState getInstance(Project project) {
        return project.getService(FactorioAutocompletionState.class);
    }
}

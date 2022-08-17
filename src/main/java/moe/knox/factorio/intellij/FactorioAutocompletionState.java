package moe.knox.factorio.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
        name = "FactorioAutocompletionConfig",
        storages = {
                @Storage("FactorioAutocompletionConfig.xml")
        }
)
public class FactorioAutocompletionState implements PersistentStateComponent<FactorioAutocompletionState> {
    public boolean integrationActive = false;
    public String curVersion = "";
    public FactorioVersion selectedFactorioVersion = FactorioVersion.createLatest();
    public String currentLualibVersion = "";

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

    public record FactorioVersion(String version, boolean latest) {
        private static final String latestVersion = "latest";

        @Override
        public String toString() {
            if (latest) {
                return "Latest version";
            }

            return version;
        }

        static FactorioVersion createLatest()
        {
            return new FactorioVersion(latestVersion, true);
        }

        static FactorioVersion createVersion(String version)
        {
            return new FactorioVersion(version, false);
        }
    }
}

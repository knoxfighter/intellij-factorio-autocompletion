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
    public FactorioVersion selectedFactorioVersion = FactorioVersion.latest();
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

    public record FactorioVersion(String desc, String link) {
        @Override
        public String toString() {
            return desc;
        }

        static FactorioVersion latest()
        {
            return new FactorioVersion("Latest version", "/latest/");
        }

        public boolean isLatest() {
            return this.desc.equals("Latest version");
        }
    }
}

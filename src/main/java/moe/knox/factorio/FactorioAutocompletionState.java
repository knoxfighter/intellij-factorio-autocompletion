package moe.knox.factorio;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
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
    public static class FactorioVersion {
        public String desc;
        public String link;

        public FactorioVersion() {
            desc = "Latest version";
            link = "/latest/";
        }

        public FactorioVersion(String desc, String link) {
            this.desc = desc;
            this.link = link;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof FactorioVersion)) {
                return false;
            }

            FactorioVersion factorioVersion = (FactorioVersion) obj;

            return factorioVersion.desc.equals(desc) && factorioVersion.link.equals(link);
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    public boolean integrationActive = false;
    public String curVersion = "";
    public FactorioVersion selectedFactorioVersion = new FactorioVersion();
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
        return ServiceManager.getService(project, FactorioAutocompletionState.class);
    }
}

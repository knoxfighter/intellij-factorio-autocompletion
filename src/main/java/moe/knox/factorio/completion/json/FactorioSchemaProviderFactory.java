package moe.knox.factorio.completion.json;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.jsonSchema.extension.JsonSchemaFileProvider;
import com.jetbrains.jsonSchema.extension.JsonSchemaProviderFactory;
import com.jetbrains.jsonSchema.extension.SchemaType;
import moe.knox.factorio.FactorioAutocompletionState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FactorioSchemaProviderFactory implements JsonSchemaProviderFactory {
    @NotNull
    @Override
    public List<JsonSchemaFileProvider> getProviders(@NotNull Project project) {
        return Arrays.asList(new FactorioSchemaFileProvider(project));
    }

    class FactorioSchemaFileProvider implements JsonSchemaFileProvider {
        private Project project;

        public FactorioSchemaFileProvider(Project project) {
            this.project = project;
        }

        @Override
        public boolean isAvailable(@NotNull VirtualFile file) {
            return FactorioAutocompletionState.getInstance(project).integrationActive && file.getName().equals("info.json");
        }

        @NotNull
        @Override
        public String getName() {
            return "Factorio mod info";
        }

        @Nullable
        @Override
        public VirtualFile getSchemaFile() {
            return JsonSchemaProviderFactory.getResourceFile(FactorioSchemaProviderFactory.class, "/mod-info-schema.json");
        }

        @NotNull
        @Override
        public SchemaType getSchemaType() {
            return SchemaType.embeddedSchema;
        }
    }
}

package moe.knox.factorio.tang.file.resolver;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import moe.knox.factorio.intellij.FactorioState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class FactorioRootDir extends FactorioFileResolver {
    @Nullable
    @Override
    public VirtualFile find(@NotNull Project project, @NotNull String shortUrl, @NotNull String[] extNames) {
        // Do nothing, if integration is deactivated
        if (!FactorioState.getInstance(project).integrationActive) {
            return null;
        }

        VirtualFile basePath = VfsUtil.findFileByIoFile(new File(project.getBasePath()), true);

        return findFile(shortUrl, basePath, extNames);
    }
}

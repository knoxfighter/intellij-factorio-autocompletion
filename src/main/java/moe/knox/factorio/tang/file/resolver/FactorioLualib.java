package moe.knox.factorio.tang.file.resolver;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.service.LuaLibService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class FactorioLualib extends FactorioFileResolver {
    @Nullable
    @Override
    public VirtualFile find(@NotNull Project project, @NotNull String shortUrl, @NotNull String[] extNames) {
        // Do nothing, if integration is deactivated
        if (!FactorioState.getInstance(project).integrationActive) {
            return null;
        }

        Path currentLuaLibPath = LuaLibService.getInstance(project).getCurrentLuaLibPath();
        if (currentLuaLibPath != null) {
            VirtualFile libraryFile = VfsUtil.findFileByIoFile(currentLuaLibPath.toFile(), true);
            return findFile(shortUrl, libraryFile, extNames);
        }

        return null;
    }
}

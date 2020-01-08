package moe.knox.factorio.library.fileResolver;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.library.FactorioLualibParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class FactorioLualib extends FactorioFileResolver {
    @Nullable
    @Override
    public VirtualFile find(@NotNull Project project, @NotNull String shortUrl, @NotNull String[] extNames) {
        // Do nothing, if integration is deactivated
        if (!FactorioAutocompletionState.getInstance(project).integrationActive) {
            return null;
        }


        String currentLualibLink = FactorioLualibParser.getCurrentLualibLink(project);
        if (currentLualibLink != null) {
            VirtualFile libraryFile = VfsUtil.findFileByIoFile(new File(currentLualibLink), true);
            return findFile(shortUrl, libraryFile, extNames);
        }

        return null;
    }
}

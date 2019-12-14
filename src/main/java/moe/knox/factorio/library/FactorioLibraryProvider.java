package moe.knox.factorio.library;

import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.util.PathUtil;
import com.intellij.util.io.URLUtil;
import com.tang.intellij.lua.lang.LuaIcons;
import com.tang.intellij.lua.psi.LuaFileUtil;
import moe.knox.factorio.FactorioAutocompletionState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class FactorioLibraryProvider extends AdditionalLibraryRootsProvider {
    @NotNull
    @Override
    public Collection<SyntheticLibrary> getAdditionalProjectLibraries(@NotNull Project project) {
        // Do nothing, if integration is deactivated
        if (!FactorioAutocompletionState.getInstance(project).integrationActive) {
            return Arrays.asList();
        }

        String jarPath = PathUtil.getJarPathForClass(FactorioLibraryProvider.class);

        // libDir for hardcoded things (builtin-types)
        VirtualFile libDir = null;
        try {
            libDir = VfsUtil.findFileByURL(URLUtil.getJarEntryURL(new File(jarPath), "library"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        for (VirtualFile libDirChild : libDir.getChildren()) {
            libDirChild.putUserData(LuaFileUtil.INSTANCE.getPREDEFINED_KEY(), true);
        }

        Collection<SyntheticLibrary> libList = new ArrayList<>();
        libList.add(new FactorioLibrary(libDir, "Factorio Builtins"));

        // libDir for downloaded factorio api
        VirtualFile dynDir = null;
        String downloadedApiDir = FactorioApiParser.getCurrentApiLink(project);
        if (downloadedApiDir != null && !downloadedApiDir.isEmpty()) {
            File downloadedApiFile = new File(downloadedApiDir);
            dynDir = VfsUtil.findFileByIoFile(downloadedApiFile, true);
            for (VirtualFile dynDirChild : dynDir.getChildren()) {
                dynDirChild.putUserData(LuaFileUtil.INSTANCE.getPREDEFINED_KEY(), true);
            }
        }

        if (dynDir != null) {
            libList.add(new FactorioLibrary(dynDir, "Factorio API"));
        }

        // protoDir for downloaded factorio prototypes
        VirtualFile protoDir = null;
        String downloadedProtoDir = FactorioPrototypeParser.getCurrentPrototypeLink(project);
        if (downloadedProtoDir != null && !downloadedProtoDir.isEmpty()) {
            File downloadedProtoFile = new File(downloadedProtoDir);
            protoDir = VfsUtil.findFileByIoFile(downloadedProtoFile, true);
            for (VirtualFile protoDirChild : protoDir.getChildren()) {
                protoDirChild.putUserData(LuaFileUtil.INSTANCE.getPREDEFINED_KEY(), true);
            }
        }

        if (protoDir != null) {
            libList.add(new FactorioLibrary(protoDir, "Factorio Prototypes"));
        }

        // return all libDirs as array
        return libList;
    }

    public static void reload() {
        WriteAction.run(() -> {
            Project[] openProjects = ProjectManagerEx.getInstanceEx().getOpenProjects();
            for (Project openProject : openProjects) {
                ProjectRootManagerEx.getInstanceEx(openProject).makeRootsChange(EmptyRunnable.getInstance(), false, true);
            }

            StubIndex.getInstance().forceRebuild(new Throwable("Factorio API changed"));
        });
    }

    class FactorioLibrary extends SyntheticLibrary implements ItemPresentation {
        VirtualFile root;
        String factorioApiVersion;

        public FactorioLibrary(VirtualFile root, String factorioApiVersion) {
            this.root = root;
            this.factorioApiVersion = factorioApiVersion;
        }

        @Override
        public int hashCode() {
            return root.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FactorioLibrary && ((FactorioLibrary) o).root == root;
        }

        @NotNull
        @Override
        public Collection<VirtualFile> getSourceRoots() {
            return Arrays.asList(root);
        }

        @Nullable
        @Override
        public String getLocationString() {
            return "Factorio library";
        }

        @Nullable
        @Override
        public Icon getIcon(boolean unused) {
            return LuaIcons.FILE;
        }

        @Nullable
        @Override
        public String getPresentableText() {
            return factorioApiVersion;
        }
    }
}

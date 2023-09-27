package moe.knox.factorio.core;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import moe.knox.factorio.core.util.FileIndexerUtil;
import moe.knox.factorio.intellij.library.service.LuaLibService;

import java.nio.file.Path;
import java.util.*;

@Service
final public class PrototypesService {
    Project project;
    List<Map<String, Set<String>>> index = new ArrayList<>();
    public PrototypesService(Project project) {
        this.project = project;
    }

    public static PrototypesService getInstance(Project project) {
        return project.getService(PrototypesService.class);
    }

    // Create index for base and core Prototypes
    public void reloadIndex() {
        Path currentPrototypePath = LuaLibService.getInstance(project).getCurrentCorePrototypePath();
        if (currentPrototypePath != null) {
            ReadAction.run(() -> {
                PsiManager psiManager = PsiManager.getInstance(project);

                VirtualFile rootFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(currentPrototypePath.toString());
                VfsUtil.iterateChildrenRecursively(
                        rootFile,
                        virtualFile -> true,
                        virtualFile -> {
                            if (virtualFile.exists() && !virtualFile.isDirectory() && virtualFile.getExtension().equals("lua")) {
                                PsiFile psiFile = psiManager.findFile(virtualFile);
                                Map<String, Set<String>> indexMap = FileIndexerUtil.generateIndexMap(psiFile);

                                index.add(indexMap);
                            }
                            return true;
                        }
                );
            });
        } else {
            index.clear();
        }
    }

    public Set<String> getValues(String prototypeType) {
        Set<String> resultList = new HashSet<>();
        for (Map<String, Set<String>> indexMap : index) {
            Set<String> prototypeNames = indexMap.get(prototypeType);
            if (prototypeNames != null) {
                resultList.addAll(prototypeNames);
            }
        }
        return resultList;
    }

    public Set<String> getAllKeys() {
        Set<String> resultList = new HashSet<>();
        for (Map<String, Set<String>> indexMap : index) {
            resultList.addAll(indexMap.keySet());
        }
        return resultList;
    }
}

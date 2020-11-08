package moe.knox.factorio.indexer;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import moe.knox.factorio.downloader.DownloaderContainer;

import java.util.*;

@Service
final public class BasePrototypesService {
    public static BasePrototypesService getInstance(Project project) {
        return ServiceManager.getService(project, BasePrototypesService.class);
    }

    Project project;
    List<Map<String, Set<String>>> index = new ArrayList<>();

    public BasePrototypesService(Project project) {
        this.project = project;
    }

    // Create index for base and core Prototypes
    public void reloadIndex() {
        String currentPrototypeLink = DownloaderContainer.getInstance(project).getCurrentPrototypeDefinitionLink();
        if (currentPrototypeLink != null) {
            ReadAction.run(() -> {
                PsiManager psiManager = PsiManager.getInstance(project);

                VirtualFile rootFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(currentPrototypeLink);
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

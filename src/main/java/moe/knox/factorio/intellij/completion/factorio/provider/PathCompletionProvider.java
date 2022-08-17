package moe.knox.factorio.intellij.completion.factorio.provider;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.lang.type.LuaString;
import moe.knox.factorio.core.parser.LualibParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.SystemIndependent;

import java.io.File;

public class PathCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet resultSet) {
        PsiElement position = parameters.getPosition();

        // Path to the root of the project
        @SystemIndependent String projectPath = position.getProject().getBasePath();

        iterateOverFiles(projectPath, position, resultSet);

        // Path to the root of the Factorio Lualib
        String currentLualibLink = LualibParser.getCurrentLualibLink(position.getProject());
        if (currentLualibLink != null) {
            iterateOverFiles(currentLualibLink, position, resultSet);
        }
    }

    private void iterateOverFiles(String rootPath, PsiElement position, CompletionResultSet resultSet) {
        // Append the current subdirectories to project root
        String filePath = "";
        String currentLink = LuaString.Companion.getContent(position.getText()).getValue();
        int lastDot = currentLink.lastIndexOf(".");
        if (lastDot > -1) {
            currentLink = currentLink.substring(0, lastDot);
            if (!currentLink.isEmpty()) {
                currentLink = currentLink.replace(".", "/");
                filePath = rootPath + "/" + currentLink;
            }
        }

        if (filePath.isEmpty()) {
            filePath = rootPath;
        }

        File file = new File(filePath);

        if (file.exists() && file.isDirectory()) {

            // Iterate over all files in this directory (only show directories and lua files)
            for (File listFile : file.listFiles(
                    file1 -> (file1.isDirectory() && !file1.getName().startsWith(".")) || getFileExtension(file1).equals("lua")
            )) {
                String relativePath = new File(rootPath).toURI().relativize(listFile.toURI()).getPath();

                // remove file extention
                String fileExtension = getFileExtension(listFile);
                if (!fileExtension.isEmpty()) {
                    relativePath = relativePath.substring(0, relativePath.indexOf(fileExtension) - 1);
                }
                relativePath = relativePath.replace("/", ".");

                LookupElementBuilder element = LookupElementBuilder.create(relativePath);
                element = element.withInsertHandler((insertionContext, lookupElement) -> {
                    Project project = insertionContext.getProject();
                    Editor editor = insertionContext.getEditor();
                    AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, null);
                });
                resultSet.addElement(element);
            }
        }
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf + 1);
    }
}

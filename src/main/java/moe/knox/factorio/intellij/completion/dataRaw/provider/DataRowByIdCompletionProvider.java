package moe.knox.factorio.intellij.completion.dataRaw.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.intellij.PrototypeFileIndexer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public class DataRowByIdCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        PsiElement position = parameters.getPosition();
        Project project = position.getProject();

        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(PrototypeFileIndexer.NAME, project);

        for (String key : allKeys) {
            if (!key.contains("-") && !key.contains(".")) {
                resultSet.addElement(new LuaLookupElement(key, false, null));
            }
        }


        Set<String> baseKeys = PrototypesService.getInstance(project).getAllKeys();
        for (String baseKey : baseKeys) {
            if (!baseKey.contains("-") && !baseKey.contains(".")) {
                resultSet.addElement(new LuaLookupElement(baseKey, false, null));
            }
        }

        resultSet.stopHere();
    }
}

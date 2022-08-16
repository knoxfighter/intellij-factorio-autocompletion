package moe.knox.factorio.intellij.completion.dataRaw;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import moe.knox.factorio.core.BasePrototypesService;
import moe.knox.factorio.intellij.PrototypeFileIndexer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public class DataRowByIndexCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        Project project = parameters.getPosition().getProject();
        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(PrototypeFileIndexer.NAME, project);

        for (String key : allKeys) {
            resultSet.addElement(new LuaLookupElement(key, false, null));
        }

        Set<String> baseKeys = BasePrototypesService.getInstance(project).getAllKeys();
        for (String baseKey : baseKeys) {
            resultSet.addElement(new LuaLookupElement(baseKey, false, null));
        }
    }
}

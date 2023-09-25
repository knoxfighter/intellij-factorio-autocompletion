package moe.knox.factorio.intellij.completion.dataRaw.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.util.FactorioTreeUtil;
import moe.knox.factorio.intellij.PrototypeFileIndexer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SubPrototypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    boolean isStringLiteral;

    public SubPrototypeCompletionProvider(boolean isStringLiteral) {
        super();
        this.isStringLiteral = isStringLiteral;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        PsiElement position = parameters.getPosition();
        Project project = position.getProject();

        // Strings are inside a Literal and therefore it has to calculated from the parent
        if (isStringLiteral) {
            position = position.getParent();
        }

        // get previous index
        LuaIndexExpr childIndexExpr = PsiTreeUtil.getPrevSiblingOfType(position, LuaIndexExpr.class);
        if (childIndexExpr != null) {
            String prototypeType = FactorioTreeUtil.getPrototypeType(childIndexExpr);

            if (prototypeType != null && !prototypeType.isEmpty()) {

                FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

                // get Prototypes from Indexer
                List<Set<String>> projectPrototypes = fileBasedIndex.getValues(PrototypeFileIndexer.NAME, prototypeType, GlobalSearchScope.projectScope(project));
                List<Set<String>> globalPrototypes = fileBasedIndex.getValues(PrototypeFileIndexer.NAME, prototypeType, GlobalSearchScope.allScope(project));
                Set<String> basePrototypes = PrototypesService.getInstance(project).getValues(prototypeType);

                // Iterate over all Prototypes and add them with LookupElements
                for (Set<String> projectPrototype : projectPrototypes) {
                    addToResult(resultSet, projectPrototype, true, 15);
                }

                for (Set<String> globalPrototype : globalPrototypes) {
                    addToResult(resultSet, globalPrototype, false, 10);
                }

                addToResult(resultSet, basePrototypes, false, 10);

                resultSet.stopHere();
            }
        }
    }

    private void addToResult(CompletionResultSet resultSet, Set<String> stringEements, boolean bold, double priority) {
        for (String stringElement : stringEements) {
            if (isStringLiteral || !stringElement.contains("-") && !stringElement.contains(".")) {
                LookupElement element = new LuaLookupElement(stringElement, bold, null);
                element = PrioritizedLookupElement.withPriority(element, priority);
                resultSet.addElement(element);
            }
        }
    }
}

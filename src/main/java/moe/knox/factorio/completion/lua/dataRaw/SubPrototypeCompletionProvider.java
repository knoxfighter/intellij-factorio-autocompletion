package moe.knox.factorio.completion.lua.dataRaw;

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
import moe.knox.factorio.indexer.PrototypeFileIndexer;
import moe.knox.factorio.util.FactorioTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class SubPrototypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    boolean isStringLiteral;

    SubPrototypeCompletionProvider(boolean isStringLiteral) {
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

                // Iterate over all Prototypes and add them with LookupElements
                for (Set<String> projectPrototype : projectPrototypes) {
                    for (String s : projectPrototype) {
                        if (isStringLiteral || !s.contains("-") && !s.contains(".")) {
                            LookupElement element = new LuaLookupElement(s, true, null);
                            element = PrioritizedLookupElement.withPriority(element, 15);
                            resultSet.addElement(element);
                        }
                    }
                }

                for (Set<String> globalPrototype : globalPrototypes) {
                    for (String s : globalPrototype) {
                        if (isStringLiteral || !s.contains("-") && !s.contains(".")) {
                            LookupElement element = new LuaLookupElement(s, false, null);
                            element = PrioritizedLookupElement.withPriority(element, 10);
                            resultSet.addElement(element);
                        }
                    }
                }

                resultSet.stopHere();
            }
        }
    }
}

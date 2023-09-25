package moe.knox.factorio.intellij.completion.factorio.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;
import com.tang.intellij.lua.ty.TyKind;
import moe.knox.factorio.intellij.PrototypeFileIndexer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class PrototypeTableCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        PsiElement element = parameters.getPosition();
        PsiElement indexExprCanditade = element.getParent().getParent();

        if (indexExprCanditade instanceof LuaIndexExpr indexExpr) {

            Project project = indexExpr.getProject();
            SearchContext searchContext = SearchContext.Companion.get(project);
            ITy type = indexExpr.guessType(searchContext);

            String typeString = "";
            if (type.getKind().equals(TyKind.Class)) {
                if (type.getDisplayName().equals("LuaTechnology")) {
                    typeString = "technology";
                } else if (type.getDisplayName().equals("LuaRecipe")) {
                    typeString = "recipe";
                }
            }

            if (!typeString.isEmpty()) {
                List<Set<String>> globalValues = FileBasedIndex.getInstance().getValues(PrototypeFileIndexer.NAME, typeString, GlobalSearchScope.allScope(project));
                List<Set<String>> projectValues = FileBasedIndex.getInstance().getValues(PrototypeFileIndexer.NAME, typeString, GlobalSearchScope.projectScope(project));

                addLookupElements(projectValues, true, 15, resultSet);
                addLookupElements(globalValues, false, 5, resultSet);
            }
        }
    }

    private void addLookupElements(@NotNull List<Set<String>> lookupStrings, boolean bold, double priority, CompletionResultSet resultSet) {
        for (Set<String> lookupString : lookupStrings) {
            for (String s : lookupString) {
                LookupElement lookupElement = new LuaLookupElement(s, bold, null);
                lookupElement = PrioritizedLookupElement.withPriority(lookupElement, priority);
                resultSet.addElement(lookupElement);
            }
        }
    }
}

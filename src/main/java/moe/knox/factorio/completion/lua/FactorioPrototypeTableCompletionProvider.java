package moe.knox.factorio.completion.lua;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
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
import moe.knox.factorio.indexer.PrototypeFileIndexer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class FactorioPrototypeTableCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        PsiElement element = parameters.getPosition();
        PsiElement indexExprCanditade = element.getParent().getParent();

        if (indexExprCanditade instanceof LuaIndexExpr) {
            LuaIndexExpr indexExpr = (LuaIndexExpr) indexExprCanditade;

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
                List<Set<String>> values = FileBasedIndex.getInstance().getValues(PrototypeFileIndexer.NAME, typeString, GlobalSearchScope.projectScope(project));

                for (Set<String> value : values) {
                    for (String s : value) {
                        resultSet.addElement(new LuaLookupElement(s, false, null));
                    }
                }

            }
        }
    }
}

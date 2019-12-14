package moe.knox.factorio.completion.lua;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.editor.completion.LookupElementFactory;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import com.tang.intellij.lua.psi.LuaClass;
import com.tang.intellij.lua.psi.LuaClassField;
import com.tang.intellij.lua.psi.LuaTableExpr;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.psi.search.LuaShortNamesManager;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITyClass;
import org.jetbrains.annotations.NotNull;

public class FactorioPrototypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
        // get table element
        LuaTableExpr table = PsiTreeUtil.getParentOfType(parameters.getPosition(), LuaTableExpr.class);
        if (table != null) {
            resultSet.stopHere();

            // get additional vars
            Project project = table.getProject();
            PrefixMatcher prefixMatcher = resultSet.getPrefixMatcher();
            SearchContext searchContext = SearchContext.Companion.get(project);

            // find the type of this table
            LuaTableField type = table.findField("type");
            if (type == null) {
                // TODO only show `type` for completion
                resultSet.addElement(new LuaLookupElement("type", true, null));
                return;
            }
            String typeText = type.getExprList().get(0).getFirstChild().getText();
            typeText = typeText.replace("\"", "");
            typeText = StringUtil.capitalizeWords(typeText, "-", true, false);
            typeText = typeText.replace(" ", "");
            typeText = "Prototype_" + typeText;

            // get the correct Class, for this prototype
            LuaClass color = LuaShortNamesManager.Companion.getInstance(project).findClass(typeText, searchContext);
            if (color == null) {
                // Do nothing, when color not found
                return;
            }
            ITyClass colorType = color.getType();

            // Iterate over all classes, this var derives from and print that completion
            colorType.eachTopClass(tyClass -> {
                tyClass.lazyInit(searchContext);
                tyClass.processMembers(searchContext, (curType, member) -> {
                    String memberName = member.getName();
                    if (prefixMatcher.prefixMatches(memberName)) {
                        String className = curType.getDisplayName();
                        if (member instanceof LuaClassField) {
                            LuaLookupElement element = LookupElementFactory.Companion.createFieldLookupElement(className, memberName, ((LuaClassField) member), null, curType == tyClass);
                            element.setHandler((insertionContext, lookupElement) -> {
                                Document document = insertionContext.getDocument();
                                Editor editor = insertionContext.getEditor();

                                document.insertString(insertionContext.getTailOffset(), " = ");
                                document.insertString(insertionContext.getTailOffset(), "\"\"");
                                editor.getCaretModel().moveToOffset(insertionContext.getTailOffset() - 1);
                                document.insertString(insertionContext.getTailOffset(), ",");

                            });
                            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 15.0));
                        }
                    }
                    return null;
                });
                return true;
            });
        }

        return;
    }
}

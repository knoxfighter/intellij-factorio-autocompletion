package moe.knox.factorio.completion.lua;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.editor.completion.LookupElementFactory;
import com.tang.intellij.lua.editor.completion.LuaFieldLookupElement;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import com.tang.intellij.lua.psi.LuaClassField;
import com.tang.intellij.lua.psi.LuaTableExpr;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.*;
import moe.knox.factorio.FactorioPrototypeTypeGuesser;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PrototypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
//        String fieldName = PsiTreeUtil.getParentOfType(parameters.getPosition(), LuaTableField.class).getFieldName();
        String fieldName = PsiTreeUtil.getParentOfType(parameters.getPosition(), LuaTableField.class, false, LuaTableExpr.class).getFieldName();
        if (fieldName != null) {
            // this is a subelement and therefore not relevant for now
        } else {
            resultSet.stopHere();

            /// The completion for the tableFieldName
            ITy luaClass = FactorioPrototypeTypeGuesser.guessType(parameters.getPosition());
            if (luaClass == null) {
                // only complete member `type`
                LuaLookupElement element = new LuaLookupElement("type", true, null);
                addInsertHandler(element);
                resultSet.addElement(element);
                return;
            } else if (luaClass.equals(TyPrimitive.Companion.getUNKNOWN())) {
                // The class is unknown and it already has a `type` field, so simply do nothing...
                return;
            }

            SearchContext searchContext = SearchContext.Companion.get(parameters.getPosition().getProject());

            LuaTableExpr table = PsiTreeUtil.getParentOfType(parameters.getPosition(), LuaTableExpr.class);

            // get list of all used fields
            List<String> luaTableFieldNames = new ArrayList<>();
            List<LuaTableField> tableFieldList = table.getTableFieldList();
            for (LuaTableField luaTableField : tableFieldList) {
                luaTableFieldNames.add(luaTableField.getName());
            }

            // Iterate over all classes, this var derives from and print that completion
            luaClass.eachTopClass(tyClass -> {
                tyClass.lazyInit(searchContext);
                tyClass.processMembers(searchContext, (curType, member) -> {
                    String memberName = member.getName();

                    PrefixMatcher prefixMatcher = resultSet.getPrefixMatcher();
                    if (prefixMatcher.prefixMatches(memberName) && !luaTableFieldNames.contains(memberName)) {
                        String className = curType.getDisplayName();
                        if (member instanceof LuaClassField) {
                            LuaLookupElement element = LookupElementFactory.Companion.createFieldLookupElement(className, memberName, ((LuaClassField) member), null, false);
                            addInsertHandler(element);
                            resultSet.addElement(PrioritizedLookupElement.withPriority(element, 15.0));
                        }
                    }
                    return null;
                });
                return true;
            });
        }
    }

    /**
     * Add ` = ` to the autocompletion.
     * Also move the cursor to the correct position to just writer further.
     *
     * @param element add the handler to
     */
    private void addInsertHandler(LuaLookupElement element) {
        element.setHandler((insertionContext, lookupElement) -> {
            Document document = insertionContext.getDocument();
            Editor editor = insertionContext.getEditor();

            SearchContext searchContext = SearchContext.Companion.get(insertionContext.getProject());

            // Get Type
            ITy type;
            if (lookupElement instanceof LuaFieldLookupElement) {
                type = ((LuaFieldLookupElement) lookupElement).getType();
            } else if (lookupElement.getLookupString().equals("type")) {
                type = TyPrimitive.Companion.getSTRING();
            } else {
                type = TyPrimitive.Companion.getNIL();
            }
            type = TyAliasSubstitutor.Companion.substitute(type, searchContext);

            document.insertString(insertionContext.getTailOffset(), " = ");

            int offsetRemoval = 0;

            if (type.getKind().equals(TyKind.Array)) {
                document.insertString(insertionContext.getTailOffset(), "{{}}");
                offsetRemoval += 2;
            } else if (type.getKind().equals(TyKind.Class)) {
                if (type instanceof  TySerializedClass && ((TySerializedClass) type).getClassName().equals("LocalisedString")) {
                    document.insertString(insertionContext.getTailOffset(), "{\"\"}");
                    offsetRemoval += 2;
                } else {
                    document.insertString(insertionContext.getTailOffset(), "{}");
                    offsetRemoval += 1;
                }
            } else if (type.getKind().equals(TyKind.Primitive)) {
                TyPrimitiveKind primitiveKind = null;
                if (type instanceof TyPrimitive) {
                    TyPrimitive tyPrimitive = (TyPrimitive) type;
                    primitiveKind = tyPrimitive.getPrimitiveKind();
                } else if (type instanceof TyPrimitiveClass) {
                    TyPrimitiveClass tyPrimitiveClass = (TyPrimitiveClass) type;
                    primitiveKind = tyPrimitiveClass.getPrimitiveKind();
                }
                if (primitiveKind != null && primitiveKind == TyPrimitiveKind.String) {
                    document.insertString(insertionContext.getTailOffset(), "\"\"");
                    offsetRemoval += 1;
                }
            }

            editor.getCaretModel().moveToOffset(insertionContext.getTailOffset() - offsetRemoval);
            document.insertString(insertionContext.getTailOffset(), ",");
        });
    }
}

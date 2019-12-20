package moe.knox.factorio;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.tang.intellij.lua.psi.LuaClass;
import com.tang.intellij.lua.psi.LuaClassMember;
import com.tang.intellij.lua.psi.LuaTableExpr;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.psi.search.LuaShortNamesManager;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;
import com.tang.intellij.lua.ty.TySerializedClass;
import org.jetbrains.annotations.Nullable;

public class FactorioPrototypeTypeGuesser {
    @Nullable
    public static ITy guessType(PsiElement element) {
        // get table element
        LuaTableExpr table = PsiTreeUtil.getParentOfType(element, LuaTableExpr.class);
        if (table == null) {
            return null;
        }

        // get additional vars
        Project project = table.getProject();
        SearchContext searchContext = SearchContext.Companion.get(project);

        String typeText;

        // find the type of this table
        LuaTableField type = table.findField("type");
        if (type == null) {
            // could be subtype .. guess type of parent
            ITy tyGuess = guessType(table);

            typeText = ((TySerializedClass) tyGuess).getClassName();
        } else {
            // get the className for this type
            typeText = type.getExprList().get(0).getFirstChild().getText();
            typeText = typeText.replace("\"", "");
            typeText = StringUtil.capitalizeWords(typeText, "-", true, false);
            typeText = typeText.replace(" ", "");
            typeText = "Prototype_" + typeText;
        }

        // get the correct Class, for this prototype
        LuaClass luaClass = LuaShortNamesManager.Companion.getInstance(project).findClass(typeText, searchContext);
        if (luaClass == null) {
            // Do nothing, when luaClass not found
            return null;
        }

        LuaTableField tableField = PsiTreeUtil.getParentOfType(element.getParent(), LuaTableField.class, false, LuaTableExpr.class);
        if (tableField != null) {
            String fieldName = tableField.getFieldName();

            if (fieldName != null && !fieldName.isEmpty()) {
                LuaClassMember member = luaClass.getType().findMember(fieldName, searchContext);
                return member.guessType(searchContext);
            }
        }

        return luaClass.getType();
    }
}

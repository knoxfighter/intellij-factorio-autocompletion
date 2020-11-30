package moe.knox.factorio.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.psi.*;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.*;

import java.util.List;

public class PrototypeTreeUtil {
    public static ITy guessPrototypeDefinitionTypes(LuaTableField element, SearchContext searchContext) {
            LuaPsiElement parent = PsiTreeUtil.getParentOfType(element, LuaTableField.class, LuaLocalDef.class, LuaAssignStat.class, LuaListArgs.class, LuaSingleArg.class);
        if (parent instanceof LuaTableField) {
            // do iterative and check for name of the field
            LuaTableField field = (LuaTableField) parent;
            field.getFieldName();
            guessPrototypeDefinitionTypes(field, searchContext);
        } else if (parent instanceof LuaListArgs) {
            // get the index of the table expression
            LuaTableExpr tableExpr = (LuaTableExpr) element.getParent();
            LuaListArgs luaListArgs = (LuaListArgs) parent;
            int index = -1;
            List<LuaExpr> exprList = luaListArgs.getExprList();
            for (int i = 0; i < exprList.size(); i++) {
                LuaExpr luaExpr = exprList.get(i);
                if (luaExpr == tableExpr) {
                    index = i;
                }
            }

            // get the type of the function call
            LuaCallExpr callExpr = (LuaCallExpr) parent.getParent();
            return  getTyFromCallExpr(callExpr, index, searchContext);
        } else if (parent instanceof LuaSingleArg) {
            // only parse the function, cause function only has one param.
            LuaCallExpr callExpr = (LuaCallExpr) parent.getParent();
            return getTyFromCallExpr(callExpr, 0, searchContext);
        } else if (parent instanceof LuaLocalDef) {
            // guess the type of a local variable definition
            LuaLocalDef localDef = (LuaLocalDef) parent;
            // get the comment, cause in the comments the type is defined
            LuaComment comment = localDef.getComment();

            // guess the type that is defined in the comments
            if (comment != null) {
                return comment.guessType(searchContext);
            }
        } else if (parent instanceof LuaAssignStat) {
            // guess the type of an assign to a variable (can also be a definition of a field of a table)
            LuaAssignStat assignStat = (LuaAssignStat) parent;
            // TODO more complicated than expected. guessType does not work for subtypes :( I think implementing a typeGuesser is the way to go.
            System.out.println(assignStat.getVarExprList().guessTypeAt(searchContext));
        }

        return Ty.Companion.getUNKNOWN();
    }

    private static ITy getTyFromCallExpr(LuaCallExpr callExpr, int paramIndex, SearchContext searchContext) {
        // get the type of the function call
        ITy iTy = callExpr.guessParentType(searchContext);
        // has to be a function
        if (iTy instanceof TySerializedFunction) {
            TySerializedFunction tyFunction = (TySerializedFunction) iTy;
            // get the param with the given index, since we want a Ty, only return the Ty
            IFunSignature mainSignature = tyFunction.getMainSignature();
            return mainSignature.getParams()[paramIndex].getTy();
        }
        return Ty.Companion.getUNKNOWN();
    }

    /**
     * This will try to get the prototypeType from the indexExpression (Will work with data.raw.type and data.raw["type"])
     *
     * @param indexExpr The Expression to search from
     * @return The PrototypeType String
     */
    public static String getPrototypeType(LuaIndexExpr indexExpr) {
        String prototypeType = null;

        // get ID (TextNode)
        PsiElement id = indexExpr.getId();
        if (id != null) {
            // get the text
            prototypeType = id.getText();
        } else {
            LuaLiteralExpr literalExpr = PsiTreeUtil.findChildOfType(indexExpr, LuaLiteralExpr.class);
            if (literalExpr != null) {
                prototypeType = literalExpr.getText();
                prototypeType = prototypeType.replace("\"", "");
            }
        }

        return prototypeType;
    }
}

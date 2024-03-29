package moe.knox.factorio.core.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaLiteralExpr;

public class FactorioTreeUtil {
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

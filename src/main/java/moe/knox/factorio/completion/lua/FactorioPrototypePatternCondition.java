package moe.knox.factorio.completion.lua;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.LuaCallExpr;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaTableExpr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FactorioPrototypePatternCondition extends PatternCondition<PsiElement> {
    public FactorioPrototypePatternCondition(@Nullable String debugMethodName) {
        super(debugMethodName);
    }

    /**
     * Only run when in subtable of table inside function call with text `data:extend`
     */
    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext context) {
        int tableExprAmount = 0;
        PsiElement localElement = psiElement.getParent();
        PsiElement secondParent = null;
        while ((localElement = localElement.getParent()) != null) {
            if (localElement instanceof LuaTableExpr) {
                ++tableExprAmount;
                if (tableExprAmount == 2) {
                    secondParent = localElement;
                }
            }
        }

        if (tableExprAmount == 2 && secondParent != null) {
            localElement = secondParent;
            while ((localElement = localElement.getParent()) != null) {
                if (localElement instanceof LuaCallExpr) {
                    break;
                }
            }
            if (localElement != null) {
                LuaCallExpr callExpr = (LuaCallExpr) localElement;
                for (PsiElement callExprChild : callExpr.getChildren()) {
                    if (callExprChild instanceof LuaIndexExpr) {
                        LuaIndexExpr indexExpr = (LuaIndexExpr) callExprChild;
                        if (indexExpr.getText().equals("data:extend")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}

package moe.knox.factorio.intellij.completion.factorio.condition;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.LuaCallExpr;
import com.tang.intellij.lua.psi.LuaNameExpr;
import org.jetbrains.annotations.NotNull;

public class PathPatternCondition extends PatternCondition<PsiElement> {
    public PathPatternCondition() {
        super(null);
    }

    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
        // Only run when string inside require
        LuaCallExpr callExpr = PsiTreeUtil.getParentOfType(psiElement, LuaCallExpr.class);

        if (callExpr != null) {
            for (PsiElement callExprChild : callExpr.getChildren()) {
                if (callExprChild instanceof LuaNameExpr) {
                    LuaNameExpr indexExpr = (LuaNameExpr) callExprChild;
                    if (indexExpr.getName().equals("require")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

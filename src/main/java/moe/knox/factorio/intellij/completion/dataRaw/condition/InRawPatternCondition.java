package moe.knox.factorio.intellij.completion.dataRaw.condition;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import org.jetbrains.annotations.NotNull;

public class InRawPatternCondition extends PatternCondition<PsiElement> {
    boolean secondChild;

    public InRawPatternCondition(boolean secondChild) {
        super(null);
        this.secondChild = secondChild;
    }

    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
        LuaIndexExpr indexExpr = PsiTreeUtil.findChildOfType(psiElement, LuaIndexExpr.class);
        if (indexExpr != null) {
            if (secondChild) {
                indexExpr = PsiTreeUtil.findChildOfType(indexExpr, LuaIndexExpr.class);
            }

            if (indexExpr != null) {
                return indexExpr.getText().equals("data.raw");
            }
        }
        return false;
    }
}

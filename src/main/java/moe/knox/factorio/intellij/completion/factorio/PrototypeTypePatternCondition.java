package moe.knox.factorio.intellij.completion.factorio;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.LuaTableField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrototypeTypePatternCondition extends PatternCondition<PsiElement> {
    public PrototypeTypePatternCondition(@Nullable String debugMethodName) {
        super(debugMethodName);
    }

    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
        LuaTableField parentOfType = PsiTreeUtil.getParentOfType(psiElement, LuaTableField.class);
        if (parentOfType != null) {
            return parentOfType.getFieldName().equals("type");
        }
        return false;
    }
}

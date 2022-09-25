package moe.knox.factorio.intellij.completion;

import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import moe.knox.factorio.intellij.FactorioState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FactorioIntegrationActiveCondition extends PatternCondition<PsiElement> {
    public FactorioIntegrationActiveCondition(@Nullable String debugMethodName) {
        super(debugMethodName);
    }

    @Override
    public boolean accepts(@NotNull PsiElement psiElement, ProcessingContext processingContext) {
        return FactorioState.getInstance(psiElement.getProject()).integrationActive;
    }
}

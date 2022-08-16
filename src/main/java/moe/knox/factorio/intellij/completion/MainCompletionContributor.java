package moe.knox.factorio.intellij.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.*;
import moe.knox.factorio.core.FactorioPrototypeState;
import moe.knox.factorio.intellij.completion.factorio.*;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class MainCompletionContributor extends CompletionContributor {
    public MainCompletionContributor() {
        extend(CompletionType.BASIC,
                psiElement()
                        .with(new FactorioIntegrationActiveCondition(null))
                        .andOr(
                                psiElement()
                                        .withParent(
                                                psiElement(LuaTypes.NAME_EXPR)
                                                        .withParent(LuaTableField.class)
                                        )
                        ).with(new PrototypePatternCondition(null, true)),
                new PrototypeCompletionProvider()
        );

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PrototypePatternCondition(null, false))
                        .with(new PrototypeTypePatternCondition(null)),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                        for (String prototypeType : FactorioPrototypeState.getInstance().getPrototypeTypes()) {
                            completionResultSet.addElement(LookupElementBuilder.create(prototypeType));
                        }
                    }
                });

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PatternCondition<PsiElement>(null) {
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
                        }),
                new PathCompletionProvider()
        );

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .withParent(
                                psiElement(LuaLiteralExpr.class)
                                        .withParent(LuaIndexExpr.class)
                        )
                        .with(new FactorioIntegrationActiveCondition(null)),
                new PrototypeTableCompletionProvider()
        );
    }
}

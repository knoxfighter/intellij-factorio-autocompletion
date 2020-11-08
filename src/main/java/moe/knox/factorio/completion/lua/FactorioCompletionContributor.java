package moe.knox.factorio.completion.lua;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PatternCondition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.*;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class FactorioCompletionContributor extends CompletionContributor {
    public FactorioCompletionContributor() {
        extend(CompletionType.BASIC,
                psiElement()
                        .with(new FactorioIntegrationActiveCondition(null))
                        .andOr(
                                psiElement()
                                        .withParent(
                                                psiElement(LuaTypes.NAME_EXPR)
                                                        .withParent(LuaTableField.class)
                                        )
                        ).with(new FactorioPrototypePatternCondition(null, true)),
                new FactorioPrototypeCompletionProvider()
        );

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new FactorioPrototypePatternCondition(null, false))
                        .with(new FactorioPrototypeTypePatternCondition(null)),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                        // TODO reenable with new prototype layout
//                        for (String prototypeType : FactorioPrototypeState.getInstance().getPrototypeTypes()) {
//                            completionResultSet.addElement(LookupElementBuilder.create(prototypeType));
//                        }
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
                new FactorioPathCompletionProvider()
        );

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .withParent(
                                psiElement(LuaLiteralExpr.class)
                                        .withParent(LuaIndexExpr.class)
                        )
                        .with(new FactorioIntegrationActiveCondition(null)),
                new FactorioPrototypeTableCompletionProvider()
        );
    }
}

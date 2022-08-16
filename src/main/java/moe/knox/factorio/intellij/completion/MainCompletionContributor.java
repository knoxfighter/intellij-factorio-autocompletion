package moe.knox.factorio.intellij.completion;

import com.intellij.codeInsight.completion.*;
import com.tang.intellij.lua.psi.*;
import moe.knox.factorio.intellij.completion.factorio.*;

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
                new PrototypeTypeCompletionProvider());

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PathPatternCondition()),
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

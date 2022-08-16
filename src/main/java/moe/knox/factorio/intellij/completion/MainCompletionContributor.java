package moe.knox.factorio.intellij.completion;

import com.intellij.codeInsight.completion.*;
import com.tang.intellij.lua.psi.*;
import moe.knox.factorio.intellij.completion.factorio.*;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class MainCompletionContributor extends CompletionContributor {
    public MainCompletionContributor() {
        // autocomplete "type" field inside "data:extend"
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

        // autocomplete string literal for "type" in "data:extend"
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PrototypePatternCondition(null, false))
                        .with(new PrototypeTypePatternCondition(null)),
                new PrototypeTypeCompletionProvider());

        // autocomplete paths
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new PathPatternCondition()),
                new PathCompletionProvider()
        );

        // autocomplete recipe and technology strings from PrototypeIndexer
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

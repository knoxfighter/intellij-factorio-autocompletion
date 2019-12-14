package moe.knox.factorio.completion.lua;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.psi.LuaTypes;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class FactorioCompletionContributor extends CompletionContributor {
    public FactorioCompletionContributor() {
        extend(CompletionType.BASIC,
                psiElement().andOr(
                        psiElement()
                                .withParent(
                                        psiElement(LuaTypes.NAME_EXPR)
                                                .withParent(LuaTableField.class)
                                ),
                        psiElement(LuaTypes.ID)
                                .withParent(LuaTableField.class)
                ).with(new FactorioPrototypePatternCondition(null)),
                new FactorioPrototypeCompletionProvider()
        );

//        extend(CompletionType.BASIC, psiElement(LuaTypes.ID).withParent(LuaNameExpr.class), new FactorioPrototypeCompletionProvider());
    }
}

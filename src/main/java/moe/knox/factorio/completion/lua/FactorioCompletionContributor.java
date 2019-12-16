package moe.knox.factorio.completion.lua;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.psi.LuaTypes;
import moe.knox.factorio.FactorioPrototypeState;
import org.jetbrains.annotations.NotNull;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class FactorioCompletionContributor extends CompletionContributor {
    public FactorioCompletionContributor() {
        extend(CompletionType.BASIC,
                psiElement().with(new FactorioIntegrationActiveCondition(null))
                        .andOr(
                                psiElement()
                                        .withParent(
                                                psiElement(LuaTypes.NAME_EXPR)
                                                        .withParent(LuaTableField.class)
                                        )
//                        psiElement(LuaTypes.ID)
//                                .withParent(LuaTableField.class)
                        ).with(new FactorioPrototypePatternCondition(null)),
                new FactorioPrototypeCompletionProvider()
        );

        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .with(new FactorioPrototypePatternCondition(null))
                        .with(new FactorioPrototypeTypePatternCondition(null)),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                        for (String prototypeType : FactorioPrototypeState.getInstance().getPrototypeTypes()) {
                            completionResultSet.addElement(LookupElementBuilder.create(prototypeType));
                        }
                    }
                });
    }
}

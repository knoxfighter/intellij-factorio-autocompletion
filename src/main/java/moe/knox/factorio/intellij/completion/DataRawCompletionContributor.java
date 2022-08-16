package moe.knox.factorio.intellij.completion;

import com.intellij.codeInsight.completion.*;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaLiteralExpr;
import com.tang.intellij.lua.psi.LuaTypes;
import moe.knox.factorio.intellij.completion.dataRaw.DataRowByIdCompletionProvider;
import moe.knox.factorio.intellij.completion.dataRaw.DataRowByIndexCompletionProvider;
import moe.knox.factorio.intellij.completion.dataRaw.InRawPatternCondition;
import moe.knox.factorio.intellij.completion.dataRaw.SubPrototypeCompletionProvider;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class DataRawCompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor {
    public DataRawCompletionContributor() {
        /// Autocompletion for data.raw.*
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.ID)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .withParent(
                                psiElement(LuaIndexExpr.class)
                                        .with(new InRawPatternCondition(false))
                        ),
                new DataRowByIdCompletionProvider()
        );

        /// Autocompletion for data.raw["*"]
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .withParent(
                                psiElement(LuaLiteralExpr.class)
                                        .withParent(
                                                psiElement(LuaIndexExpr.class)
                                                        .with(new InRawPatternCondition(false))
                                        )
                        ),
                new DataRowByIndexCompletionProvider()
        );

        /// Autocompletion for data.raw.type.*
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.ID)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .withParent(
                                psiElement(LuaIndexExpr.class)
                                        .with(new InRawPatternCondition(true))
                        ),
                new SubPrototypeCompletionProvider(false)
        );

        /// Autocompletion for data.raw.type["*"]
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.STRING)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .withParent(
                                psiElement(LuaLiteralExpr.class)
                                        .withParent(
                                                psiElement(LuaIndexExpr.class)
                                                        .with(new InRawPatternCondition(true))
                                        )
                        ),
                new SubPrototypeCompletionProvider(true)
        );
    }
}

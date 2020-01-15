package moe.knox.factorio.completion.lua.dataRaw;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.util.indexing.FileBasedIndex;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaLiteralExpr;
import com.tang.intellij.lua.psi.LuaTypes;
import moe.knox.factorio.completion.lua.FactorioIntegrationActiveCondition;
import moe.knox.factorio.indexer.PrototypeFileIndexer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public class DataRawCompletionContributor extends CompletionContributor {
    public DataRawCompletionContributor() {
        /// Autocompletion for data.raw.*
        extend(CompletionType.BASIC,
                psiElement(LuaTypes.ID)
                        .with(new FactorioIntegrationActiveCondition(null))
                        .withParent(
                                psiElement(LuaIndexExpr.class)
                                        .with(new InRawPatternCondition(false))
                        ),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                        PsiElement position = parameters.getPosition();
                        Project project = position.getProject();

                        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(PrototypeFileIndexer.NAME, project);

                        for (String key : allKeys) {
                            if (!key.contains("-") && !key.contains(".")) {
                                resultSet.addElement(new LuaLookupElement(key, false, null));
                            }
                        }

                        resultSet.stopHere();
                    }
                }
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
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet resultSet) {
                        Project project = parameters.getPosition().getProject();
                        Collection<String> allKeys = FileBasedIndex.getInstance().getAllKeys(PrototypeFileIndexer.NAME, project);

                        for (String key : allKeys) {
                            resultSet.addElement(new LuaLookupElement(key, false, null));
                        }
                    }
                }
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

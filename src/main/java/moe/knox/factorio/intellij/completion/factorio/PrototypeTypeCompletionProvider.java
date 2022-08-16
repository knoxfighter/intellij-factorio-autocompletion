package moe.knox.factorio.intellij.completion.factorio;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import moe.knox.factorio.core.FactorioPrototypeState;
import org.jetbrains.annotations.NotNull;

public class PrototypeTypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        for (String prototypeType : FactorioPrototypeState.getInstance().getPrototypeTypes()) {
            completionResultSet.addElement(LookupElementBuilder.create(prototypeType));
        }
    }
}

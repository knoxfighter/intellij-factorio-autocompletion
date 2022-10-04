package moe.knox.factorio.intellij.completion.factorio.provider;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ProcessingContext;
import moe.knox.factorio.intellij.library.service.PrototypeService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class PrototypeTypeCompletionProvider extends CompletionProvider<CompletionParameters> {
    private static final Logger LOG = Logger.getInstance(PrototypeTypeCompletionProvider.class);

    @Override
    protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        Project project = completionParameters.getEditor().getProject();

        try {
            List<String> prototypeTypes = PrototypeService.getInstance(project).parsePrototypeTypes();

            for (String prototypeType : prototypeTypes) {
                completionResultSet.addElement(LookupElementBuilder.create(prototypeType));
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }
}

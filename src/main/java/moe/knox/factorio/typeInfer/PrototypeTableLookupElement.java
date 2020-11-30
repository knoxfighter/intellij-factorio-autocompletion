package moe.knox.factorio.typeInfer;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.lookup.LookupElement;
import com.tang.intellij.lua.editor.completion.LuaLookupElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class PrototypeTableLookupElement extends LuaLookupElement {
    public PrototypeTableLookupElement(@NotNull final String lookupString,
                             @Nullable final String tailText,
                             @Nullable final String typeText, final boolean bold,
                             @Nullable final Icon icon,
                             @NotNull final InsertHandler<LookupElement> handler) {
        super(lookupString, bold, icon);
        setTailText(tailText);
        setTypeText(typeText);
        setHandler(handler);
    }
}

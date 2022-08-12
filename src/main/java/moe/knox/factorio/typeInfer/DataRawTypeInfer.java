package moe.knox.factorio.typeInfer;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.indexing.FileBasedIndex;
import com.tang.intellij.lua.ext.ILuaTypeInfer;
import com.tang.intellij.lua.psi.*;
import com.tang.intellij.lua.psi.search.LuaShortNamesManager;
import com.tang.intellij.lua.search.RecursionGuardsKt;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.DeclarationsKt;
import com.tang.intellij.lua.ty.ITy;
import com.tang.intellij.lua.ty.ITyClass;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.indexer.PrototypeDefIndexer;
import moe.knox.factorio.util.FactorioTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class DataRawTypeInfer implements ILuaTypeInfer {
    @Nullable
    @Override
    public ITy inferType(@NotNull LuaTypeGuessable luaTypeGuessable, @NotNull SearchContext searchContext) {
        return RecursionGuardsKt.withRecursionGuard("inferType", luaTypeGuessable, () -> {
            //noinspection KotlinInternalInJava
            ITy iTy = DeclarationsKt.inferInner(luaTypeGuessable, searchContext);

            // integration active
            if (FactorioAutocompletionState.getInstance(luaTypeGuessable.getProject()).integrationActive) {
                if (iTy != null) {
                    // get the FileBasedIndex to get all Prototype defs
                    FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

                    // check if given tyClass is a PrototypeDef
                    if (fileBasedIndex.getAllKeys(PrototypeDefIndexer.NAME, searchContext.getProject()).contains(iTy.getDisplayName())) {
                        // get next non-whitespace element, has to be '='
                        PsiElement nextSiblingNonWhitespace = FactorioTreeUtil.findNextSiblingNonWhitespace(luaTypeGuessable);
                        if (((LeafPsiElement) nextSiblingNonWhitespace).getChars().equals("=")) {
                            // get next non-whitespace element, has to be a LuaExprList
                            nextSiblingNonWhitespace = FactorioTreeUtil.findNextSiblingNonWhitespace(nextSiblingNonWhitespace);
                            if (nextSiblingNonWhitespace instanceof LuaExprList) {
                                // get first element of the expression list, it has to be a LuaTableExpr `{}`
                                PsiElement firstChild = nextSiblingNonWhitespace.getFirstChild();
                                if (firstChild instanceof LuaTableExpr) {
                                    LuaTableExpr tableExpr = (LuaTableExpr) firstChild;

                                    // find field `type`
                                    LuaTableField type = tableExpr.findField("type");
                                    if (type != null) {
                                        // get the actual value of the `type` field
                                        String value = type.getValueExpr().getText();
                                        value = StringUtil.unquoteString(value);

                                        // get prototype values for tyName
                                        List<Map<String, String>> prototypeValues = fileBasedIndex.getValues(PrototypeDefIndexer.NAME, iTy.getDisplayName(), searchContext.getScope());
                                        if (prototypeValues != null && prototypeValues.size() > 0) {
                                            // iterate over all the prototype values
                                            for (Map<String, String> prototypeValue : prototypeValues) {
                                                // get actual type as string by the value
                                                String actualTypeString = prototypeValue.get(value);
                                                if (actualTypeString != null) {
                                                    // get the class from the actual type
                                                    LuaClass luaClass = LuaShortNamesManager.Companion.getInstance(luaTypeGuessable.getProject()).findClass(searchContext, actualTypeString);
                                                    if (luaClass != null) {
                                                        // get the type of the class
                                                        ITyClass classType = luaClass.getType();
                                                        if (classType != null) {
                                                            return classType;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return null;
        });
    }
}

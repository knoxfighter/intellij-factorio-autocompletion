package moe.knox.factorio.tang;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.tang.intellij.lua.ext.ILuaTypeInfer;
import com.tang.intellij.lua.psi.LuaClass;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaTypeGuessable;
import com.tang.intellij.lua.psi.search.LuaShortNamesManager;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITy;
import com.tang.intellij.lua.ty.Ty;
import moe.knox.factorio.core.util.FactorioTreeUtil;
import moe.knox.factorio.intellij.FactorioState;
import org.jetbrains.annotations.NotNull;

public class DataRawTypeInfer implements ILuaTypeInfer {
    @NotNull
    @Override
    public ITy inferType(@NotNull LuaTypeGuessable luaTypeGuessable, @NotNull SearchContext searchContext) {
        Project project = searchContext.getProject();

        if (!FactorioState.getInstance(project).integrationActive) {
            return Ty.Companion.getUNKNOWN();
        }

        // first literalExpr contains the prototype type
        LuaIndexExpr firstIndexExpr = PsiTreeUtil.findChildOfType(luaTypeGuessable, LuaIndexExpr.class);

        if (firstIndexExpr != null) {
            // second literalExpr has to be `data.raw`
            LuaIndexExpr secondIndexExpr = PsiTreeUtil.findChildOfType(firstIndexExpr, LuaIndexExpr.class);
            if (secondIndexExpr != null && secondIndexExpr.getText().equals("data.raw")) {
                String prototypeType = FactorioTreeUtil.getPrototypeType(firstIndexExpr);

                prototypeType = StringUtil.capitalizeWords(prototypeType, "-", true, false);
                prototypeType = prototypeType.replace(" ", "");
                prototypeType = "Prototype_" + prototypeType;

                LuaClass luaClass = LuaShortNamesManager.Companion.getInstance(project).findClass(prototypeType, searchContext);
                return luaClass.getType();
            }
        }

        return Ty.Companion.getUNKNOWN();
    }
}

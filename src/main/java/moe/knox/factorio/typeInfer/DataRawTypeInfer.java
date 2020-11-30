package moe.knox.factorio.typeInfer;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.ext.ILuaTypeInfer;
import com.tang.intellij.lua.psi.*;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.*;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.prototypeDefinition.PrototypeDefinitionService;
import moe.knox.factorio.prototypeDefinition.types.Prototype;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.Lua;

import java.util.List;

public class DataRawTypeInfer implements ILuaTypeInfer {
    public ITy guessParent(LuaTypeGuessable guessable, SearchContext searchContext) {
        LuaPsiElement parent = PsiTreeUtil.getParentOfType(guessable, LuaTableField.class, LuaTableExpr.class, LuaLocalDef.class, LuaAssignStat.class, LuaListArgs.class, LuaSingleArg.class);
        if (parent instanceof LuaTypeGuessable) {
            return ((LuaTypeGuessable) parent).guessType(searchContext);
        } else if (parent instanceof LuaListArgs) {
            // get the index of the table expression
            LuaListArgs luaListArgs = (LuaListArgs) parent;
            int index = -1;
            List<LuaExpr> exprList = luaListArgs.getExprList();
            for (int i = 0; i < exprList.size(); i++) {
                LuaExpr luaExpr = exprList.get(i);
                if (luaExpr.equals(guessable)) {
                    index = i;
                }
            }

            // get the type of the function call
            LuaCallExpr callExpr = (LuaCallExpr) parent.getParent();
            return getTyFromCallExpr(callExpr, index, searchContext);
        } else if (parent instanceof LuaSingleArg) {
            // only parse the function, cause function only has one param.
            LuaCallExpr callExpr = (LuaCallExpr) parent.getParent();
            return getTyFromCallExpr(callExpr, 0, searchContext);
        } else if (parent instanceof LuaLocalDef) {
            // guess the type of a local variable definition
            LuaLocalDef localDef = (LuaLocalDef) parent;
            // get the comment, cause in the comments the type is defined
            LuaComment comment = localDef.getComment();

            // guess the type that is defined in the comments
            if (comment != null) {
                return comment.guessType(searchContext);
            }
        } else if (parent instanceof LuaAssignStat) {
            // TODO implement this correctly (currently only supports simple reusage of variable)
            LuaAssignStat assignStat = (LuaAssignStat) parent;
            return assignStat.getVarExprList().guessTypeAt(searchContext);
        }

        return Ty.Companion.getUNKNOWN();
    }

    private static ITy getTyFromCallExpr(LuaCallExpr callExpr, int paramIndex, SearchContext searchContext) {
        // get the type of the function call
        ITy iTy = callExpr.guessParentType(searchContext);
        // has to be a function
        if (iTy instanceof TySerializedFunction) {
            TySerializedFunction tyFunction = (TySerializedFunction) iTy;
            // get the param with the given index, since we want a Ty, only return the Ty
            IFunSignature mainSignature = tyFunction.getMainSignature();
            return mainSignature.getParams()[paramIndex].getTy();
        }
        return Ty.Companion.getUNKNOWN();
    }

    public ITy expandPrototypeTy(ITy origTy, Project project, LuaTableExpr tableExpr) {
        if (origTy instanceof ITyClass) {
            // this ty is a class and therefore can also be a prototype
            String className = ((ITyClass) origTy).getClassName();
            Prototype prototype = PrototypeDefinitionService.getInstance(project).getPrototypeByName(className);
            if (prototype != null) {
                return new TyPrototype(prototype, tableExpr);
            }
        }
        return origTy;
    }

    @NotNull
    @Override
    public ITy inferType(@NotNull LuaTypeGuessable luaTypeGuessable, @NotNull SearchContext searchContext) {
        Project project = searchContext.getProject();

        if (!FactorioAutocompletionState.getInstance(project).integrationActive) {
            return Ty.Companion.getUNKNOWN();
        }

        if (luaTypeGuessable instanceof LuaTableField) {
            LuaTableField tableField = (LuaTableField) luaTypeGuessable;
            String fieldName = tableField.getFieldName();
            ITy parentTy = guessParent(luaTypeGuessable, searchContext);
            if (fieldName == null && parentTy instanceof TyArray) {
                ITy baseTy = ((TyArray) parentTy).getBase();
                return baseTy;
            }
        } else if (luaTypeGuessable instanceof LuaTableExpr) {
            LuaTableExpr tableExpr = (LuaTableExpr) luaTypeGuessable;
            ITy parentTy = guessParent(luaTypeGuessable, searchContext);
            return expandPrototypeTy(parentTy, project, tableExpr);
        } else {
            return Ty.Companion.getUNKNOWN();
        }


//        LuaTypeGuessable guessableParent = PsiTreeUtil.getParentOfType(luaTypeGuessable.getParent(), LuaTypeGuessable.class);
//        if (luaTypeGuessable instanceof LuaTableExpr) {
//            // this is a table Expr, so we have something to do
//            ITy iTy = guessableParent.guessType(searchContext);
//        } else if (luaTypeGuessable instanceof LuaTableField) {
//            LuaTableField tableField = (LuaTableField) luaTypeGuessable;
//            String fieldName = tableField.getFieldName();
//            ITy iTy = guessableParent.guessType(searchContext);
//        }
//        if (luaTypeGuessable instanceof LuaTableField) {
//            LuaTableField tableField = (LuaTableField) luaTypeGuessable;
//            tableField.getFieldName();
//            tableField.guessParentType(searchContext);
//        }

//        // first literalExpr contains the prototype type
//        LuaIndexExpr firstIndexExpr = PsiTreeUtil.findChildOfType(luaTypeGuessable, LuaIndexExpr.class);
//
//        if (firstIndexExpr != null) {
//            // second literalExpr has to be `data.raw`
//            LuaIndexExpr secondIndexExpr = PsiTreeUtil.findChildOfType(firstIndexExpr, LuaIndexExpr.class);
//            if (secondIndexExpr != null && secondIndexExpr.getText().equals("data.raw")) {
//                String prototypeType = FactorioTreeUtil.getPrototypeType(firstIndexExpr);
//
//                prototypeType = StringUtil.capitalizeWords(prototypeType, "-", true, false);
//                prototypeType = prototypeType.replace(" ", "");
//                prototypeType = "Prototype_" + prototypeType;
//
//                LuaClass luaClass = LuaShortNamesManager.Companion.getInstance(project).findClass(prototypeType, searchContext);
//                return luaClass.getType();
//            }
//        }

        return Ty.Companion.getUNKNOWN();
    }
}

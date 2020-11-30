package moe.knox.factorio.typeInfer;

import com.intellij.util.Consumer;
import com.tang.intellij.lua.psi.LuaClassMember;
import com.tang.intellij.lua.psi.LuaTableExpr;
import com.tang.intellij.lua.psi.LuaTableField;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITyClass;
import com.tang.intellij.lua.ty.TyClass;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import moe.knox.factorio.prototypeDefinition.PrototypeDefinitionService;
import moe.knox.factorio.prototypeDefinition.types.Prototype;
import org.jetbrains.annotations.NotNull;

public class TyPrototype extends TyClass {
    Prototype prototype;
    Prototype subPrototype;
    LuaTableExpr tableExpr;
    PrototypeDefinitionService prototypeDefinitionService;

    public TyPrototype(Prototype prot, LuaTableExpr expr) {
        super(prot.name, prot.name, "");
        prototype = prot;
        tableExpr = expr;
        prototypeDefinitionService = PrototypeDefinitionService.getInstance(expr.getProject());

        if (prototype.type.equals("prototype")) {
            LuaTableField type = expr.findField("type");
            String typeVal = type.getExprList().get(0).getFirstChild().getText();
            typeVal = typeVal.replace("\"", "");
            String subPrototypeName = prototype.stringMap.get(typeVal);
            subPrototype = prototypeDefinitionService.getPrototypeByName(subPrototypeName);
        }
    }

    private void processMembers(@NotNull Consumer<String> consumer, Prototype prot) {
        if (prot == null) {
            return;
        }
        switch (prot.type) {
            case "prototype":
                consumer.consume("type");
                processMembers(consumer, subPrototype);
                break;
            case "table":
                prot.table.properties.forEach(property -> {
                    consumer.consume(property.name);
                });
                if (prot.table.parent != null) {
                    for (String parent : prot.table.parent.split(":")) {
                        processMembers(consumer, prototypeDefinitionService.getPrototypeByName(parent));
                    }
                }
        }
    }

    public void processMembers(@NotNull Consumer<String> consumer) {
        processMembers(consumer, prototype);
    }

    @Override
    public void processMembers(@NotNull SearchContext context, @NotNull Function2<? super ITyClass, ? super LuaClassMember, Unit> processor) {
        super.processMembers(context, processor);
    }
}

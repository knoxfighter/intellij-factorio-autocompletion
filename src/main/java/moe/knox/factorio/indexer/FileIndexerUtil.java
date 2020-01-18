package moe.knox.factorio.indexer;

import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaTableExpr;
import com.tang.intellij.lua.psi.LuaTableField;

import java.util.*;

public class FileIndexerUtil {
    public static Map<String, Set<String>> generateIndexMap(PsiFile psiFile) {
        Map<String, Set<String>> map = new HashMap<>();

        // find all LuaIndexExpressions in this file
        Collection<LuaIndexExpr> indexChildren = PsiTreeUtil.findChildrenOfType(psiFile, LuaIndexExpr.class);
        for (LuaIndexExpr indexChild : indexChildren) {
            // only run, when "data:extend"
            if (indexChild.getText().equals("data:extend")) {
                // find first table expression (data:extend has always a parent table)
                LuaTableExpr firstTable = PsiTreeUtil.findChildOfType(indexChild.getParent(), LuaTableExpr.class);
                if (firstTable != null) {
                    // get all subtables of prototype definition type
                    Collection<LuaTableExpr> secondTables = PsiTreeUtil.findChildrenOfType(firstTable, LuaTableExpr.class);
                    for (LuaTableExpr secondTable : secondTables) {
                        // get type of the prototype
                        LuaTableField type = secondTable.findField("type");
                        if (type != null) {
                            String typeText = type.getExprList().get(0).getFirstChild().getText();
                            typeText = typeText.replace("\"", "");

                            // get name of the prototypes
                            LuaTableField name = secondTable.findField("name");
                            if (name != null) {
                                String nameText = name.getExprList().get(0).getFirstChild().getText();
                                nameText = nameText.replace("\"", "");

                                // save type and name to the map
                                // check if already in map
                                Set<String> mapElem = map.get(typeText);
                                if (mapElem == null) {
                                    Set<String> set = new HashSet<>();
                                    set.add(nameText);
                                    map.put(typeText, set);
                                } else {
                                    mapElem.add(nameText);
                                }
                            }
                        }
                    }
                }
            }
        }
        return map;
    }
}

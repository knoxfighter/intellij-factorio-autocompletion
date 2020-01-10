package moe.knox.factorio.indexer;

import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.tang.intellij.lua.lang.LuaFileType;
import com.tang.intellij.lua.lang.LuaLanguage;
import com.tang.intellij.lua.psi.LuaIndexExpr;
import com.tang.intellij.lua.psi.LuaTableExpr;
import com.tang.intellij.lua.psi.LuaTableField;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

// FileBasedIndex.getInstance().getValues(Test.NAME, "recipe", GlobalSearchScope.projectScope(parameters.getPosition().getProject()))

public class PrototypeFileIndexer extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> NAME = ID.create("lua.call.string.param");

    @NotNull
    @Override
    public ID<String, Set<String>> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return new DataIndexer<>() {
            @NotNull
            @Override
            public Map<String, Set<String>> map(@NotNull FileContent fileContent) {
                Map<String, Set<String>> map = new HashMap<>();

                // find all LuaIndexExpressions in this file
                Collection<LuaIndexExpr> indexChildren = PsiTreeUtil.findChildrenOfType(fileContent.getPsiFile(), LuaIndexExpr.class);
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
        };
    }

    @NotNull
    @Override
    public KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public DataExternalizer<Set<String>> getValueExternalizer() {
        return new DataExternalizer<>() {
            @Override
            public void save(@NotNull DataOutput dataOutput, Set<String> prototypeNames) throws IOException {
                dataOutput.writeInt(prototypeNames.size());
                for (String prototypeName : prototypeNames) {
                    dataOutput.writeUTF(prototypeName);
                }
            }

            @Override
            public Set<String> read(@NotNull DataInput dataInput) throws IOException {
                int size = dataInput.readInt();
                Set<String> set = new HashSet<>();

                for (int i = 0; i < size; i++) {
                    set.add(dataInput.readUTF());
                }

                return set;
            }
        };
    }

    @Override
    public int getVersion() {
        return LuaLanguage.INDEX_VERSION;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(LuaFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}

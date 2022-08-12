package moe.knox.factorio.indexer;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.tang.intellij.lua.comment.psi.LuaDocTagClass;
import com.tang.intellij.lua.comment.psi.api.LuaComment;
import com.tang.intellij.lua.lang.LuaFileType;
import com.tang.intellij.lua.lang.LuaLanguage;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PrototypeDefIndexer extends FileBasedIndexExtension {
    public static final ID<String, Map<String, String>> NAME = ID.create("factorio.prototypeDef");

    @Override
    public @NotNull ID getName() {
        return NAME;
    }

    @Override
    public @NotNull DataIndexer<String, Map<String, String>, FileContent> getIndexer() {
        return inputData -> {
            Map<String, Map<String, String>> allData = new HashMap<>();

            PsiFile psiRoot = inputData.getPsiFile();
            Collection<LuaComment> luaComments = PsiTreeUtil.collectElementsOfType(psiRoot, LuaComment.class);


            for (LuaComment luaComment : luaComments) {
                Map<String, String> prototypeValues = new HashMap<>();

                Collection<PsiErrorElement> errorChildren = PsiTreeUtil.findChildrenOfType(luaComment, PsiErrorElement.class);
                for (PsiErrorElement errorChild : errorChildren) {
                    if (errorChild.getText().equals("prototype")) {
                        PsiElement textElement = errorChild.getNextSibling();
                        if (textElement != null) {
                            String prototypeText = textElement.getText();
                            if (prototypeText != null) {
                                String[] prototypeTextSplitted = prototypeText.split(" ");
                                if (prototypeTextSplitted.length >= 3) {
                                    prototypeValues.put(prototypeTextSplitted[1], prototypeTextSplitted[2]);
                                }
                            }
                        }
                    }
                }

                if (prototypeValues.size() > 0) {
                    LuaDocTagClass tagClass = luaComment.getTagClass();
                    if (tagClass != null) {
                        allData.put(tagClass.getName(), prototypeValues);
                    }
                }
            }
            return allData;
        };
    }

    @Override
    public @NotNull KeyDescriptor<String> getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public @NotNull DataExternalizer<Map<String, String>> getValueExternalizer() {
        return new DataExternalizer<>() {
            @Override
            public void save(@NotNull DataOutput out, Map<String, String> value) throws IOException {
                out.writeInt(value.size());
                for (Map.Entry<String, String> entry : value.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    out.writeUTF(key);
                    out.writeUTF(val);
                }
            }

            @Override
            public Map<String, String> read(@NotNull DataInput in) throws IOException {
                int size = in.readInt();
                Map<String, String> map = new HashMap<>();

                for (int i = 0; i < size; i++) {
                    map.put(in.readUTF(), in.readUTF());
                }

                return map;
            }
        };
    }

    @Override
    public int getVersion() {
        return LuaLanguage.INDEX_VERSION;
    }

    @Override
    public FileBasedIndex.@NotNull InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(LuaFileType.INSTANCE);
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }
}

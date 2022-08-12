package moe.knox.factorio.indexer;

import com.intellij.util.indexing.*;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.tang.intellij.lua.lang.LuaFileType;
import com.tang.intellij.lua.lang.LuaLanguage;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// FileBasedIndex.getInstance().getValues(Test.NAME, "recipe", GlobalSearchScope.projectScope(parameters.getPosition().getProject()))

public class PrototypeFileIndexer extends FileBasedIndexExtension<String, Set<String>> {
    public static final ID<String, Set<String>> NAME = ID.create("factorio.prototypes");

    @NotNull
    @Override
    public ID<String, Set<String>> getName() {
        return NAME;
    }

    @NotNull
    @Override
    public DataIndexer<String, Set<String>, FileContent> getIndexer() {
        return fileContent -> FileIndexerUtil.generateIndexMap(fileContent.getPsiFile());
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

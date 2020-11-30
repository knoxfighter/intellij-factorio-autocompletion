package moe.knox.factorio.nameManager;

import com.intellij.openapi.project.Project;
import com.intellij.util.Processor;
import com.tang.intellij.lua.psi.LuaClass;
import com.tang.intellij.lua.psi.LuaClassMember;
import com.tang.intellij.lua.psi.search.LuaShortNamesManager;
import com.tang.intellij.lua.search.SearchContext;
import com.tang.intellij.lua.ty.ITyClass;
import moe.knox.factorio.prototypeDefinition.PrototypeDefinitionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

public class PrototypeNameManager extends LuaShortNamesManager {
    @Nullable
    @Override
    public LuaClass findClass(@NotNull String s, @NotNull SearchContext searchContext) {
        return null;
    }

    @Nullable
    @Override
    public LuaClassMember findMember(@NotNull ITyClass iTyClass, @NotNull String s, @NotNull SearchContext searchContext) {
        return null;
    }

    @NotNull
    @Override
    public Collection<LuaClassMember> getClassMembers(@NotNull String s, @NotNull SearchContext searchContext) {
        return Arrays.asList();
    }

    @Override
    public boolean processAllClassNames(@NotNull Project project, @NotNull Processor<String> processor) {
        return PrototypeDefinitionService.getInstance(project).processAllKeys(processor);
    }

    @Override
    public boolean processAllMembers(@NotNull ITyClass iTyClass, @NotNull String s, @NotNull SearchContext searchContext, @NotNull Processor<LuaClassMember> processor) {
        return false;
    }

    @Override
    public boolean processClassesWithName(@NotNull String s, @NotNull SearchContext searchContext, @NotNull Processor<LuaClass> processor) {
        return false;
    }
}

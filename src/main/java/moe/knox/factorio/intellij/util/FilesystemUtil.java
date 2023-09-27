package moe.knox.factorio.intellij.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class FilesystemUtil {
    private static final PluginId PLUGIN_ID = PluginId.getId("moe.knox.factorio.autocompletion");

    public static @NotNull Path getPluginDir()
    {
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PLUGIN_ID);

        if (descriptor == null) {
            throw new RuntimeException("Unexpected error. Plugin dir not found");
        }

        return descriptor.getPluginPath();
    }
}

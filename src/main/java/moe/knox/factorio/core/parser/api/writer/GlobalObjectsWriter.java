package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.GlobalObject;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import static moe.knox.factorio.core.parser.api.writer.GeneralWriter.*;

public class GlobalObjectsWriter {
    /**
     * Output format:
     * <pre>
     * {@code
     * --- <description>
     * ---@type <type>
     * <name> = {}
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * --- Allows inter-mod communication by way of providing a repository of interfaces that is shared by all mods.
     * ---@type LuaRemote
     * remote = {}
     * }
     * </pre>
     */
    static void writeGlobalsObjects(@NotNull Writer output, @NotNull List<GlobalObject> globalObjects) throws IOException {
        writeHeaderBlock(output, "Global objects");

        // global objects
        for (GlobalObject globalObject : globalObjects) {
            writeDescLine(output, globalObject.description);
            writeType(output, globalObject.type);
            writeObjDef(output, globalObject.name);
        }
        output.append(NEW_LINE);
    }
}

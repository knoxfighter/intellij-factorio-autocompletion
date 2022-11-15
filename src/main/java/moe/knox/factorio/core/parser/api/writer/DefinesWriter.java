package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.BasicMember;
import moe.knox.factorio.api.parser.data.Define;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import static moe.knox.factorio.core.parser.api.writer.GeneralWriter.*;

public class DefinesWriter {
    /**
     * Output format:
     * <pre>
     * {@code
     * ---@class defines
     * defines = {}
     *
     * ---@class defines.<subtype>
     * defines.<subtype> = {}
     *
     * --- <description>
     * ---@type nil
     * defines.<subtype>.<endtype> = nil
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * ---@class defines
     * defines = {}
     *
     * ---@class defines.behavior_result
     * defines.command = {}
     *
     * --- Go to a position and build a base there.
     * ---@type nil
     * defines.command.build_base = nil
     * }
     * </pre>
     */
    static void writeDefines(Writer output, @NotNull List<Define> defines) throws IOException {
        writeHeaderBlock(output, "Defines");

        output.append("---@class defines").append(NEW_LINE);
        output.append("defines = {}").append(NEW_LINE).append(NEW_LINE);
        writeDefines(output, defines, "defines");
    }

    /**
     * Output format:
     * <pre>
     * {@code
     * ---@class defines.<subtype>
     * defines.<subtype> = {}
     *
     * --- <description>
     * ---@type nil
     * defines.<subtype>.<endtype> = nil
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * ---@class defines.behavior_result
     * defines.command = {}
     *
     * --- Go to a position and build a base there.
     * ---@type nil
     * defines.command.build_base = nil
     * }
     * </pre>
     */
    private static void writeDefines(Writer output, @NotNull List<Define> defines, String parents) throws IOException {
        for (Define define : defines) {
            writeDescLine(output, define.description);

            StringWriter subDefine = new StringWriter();
            subDefine.append(parents).append('.').append(define.name);
            writeClass(output, subDefine.toString());
            writeObjDef(output, subDefine.toString());

            if (define.subkeys != null && !define.subkeys.isEmpty()) {
                writeDefines(output, define.subkeys, subDefine.toString());
            }
            if (define.values != null && !define.values.isEmpty()) {
                writeDefineValues(output, define.values, subDefine.toString());
            }
        }
        output.append(NEW_LINE);
    }

    /**
     * Output format:
     * <pre>
     * {@code
     * --- <description>
     * ---@type nil
     * defines.<subtype>.<endtype> = nil
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * --- Go to a position and build a base there.
     * ---@type nil
     * defines.command.build_base = nil
     * }
     * </pre>
     */
    private static void writeDefineValues(Writer output, @NotNull List<BasicMember> defines, String parents) throws IOException {
        for (BasicMember define : defines) {
            writeDescLine(output, define.description);
            writeType(output, "nil");
            writeValDef(output, define.name, parents);
            output.append(NEW_LINE);
        }
    }
}

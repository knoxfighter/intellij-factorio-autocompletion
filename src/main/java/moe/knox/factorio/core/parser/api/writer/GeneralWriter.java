package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.ValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static moe.knox.factorio.core.parser.api.writer.TypeResolver.getType;

public class GeneralWriter {
    static final String NEW_LINE = System.lineSeparator();

    static void writeHeaderBlock(@NotNull Writer output, @NotNull String blockName) throws IOException {
        output.append(NEW_LINE)
                .append("----------------------------------------------").append(NEW_LINE)
                .append("-".repeat(10)).append(" ").append(blockName).append(NEW_LINE)
                .append("----------------------------------------------").append(NEW_LINE)
                .append(NEW_LINE).append(NEW_LINE).append(NEW_LINE)
        ;
    }

    static void writeDescLine(@NotNull Writer output, @NotNull String line) throws IOException {
        if (!line.isEmpty()) {
            line = line.replace('\n', ' ');
            output.append("--- ").append(line).append(NEW_LINE);
        }
    }

    static void writeDescLine(Writer output, @Nullable List<String> lines) throws IOException {
        if (lines != null && !lines.isEmpty()) {
            for (String line : lines) {
                writeEmptyLine(output);
                writeDescLine(output, line);
            }
        }
    }

    static void writeEmptyLine(Writer output) throws IOException {
        output.append("---").append(NEW_LINE);
    }

    static void writeType(Writer output, String type) throws IOException {
        writeType(output, type, false);
    }

    static void writeType(Writer output, ValueType type, String name) throws IOException {
        writeType(output, getType(type, name), false);
    }

    static void writeType(Writer output, ValueType type, String name, boolean optional) throws IOException {
        writeType(output, getType(type, name), optional);
    }

    static void writeType(Writer output, String type, boolean optional) throws IOException {
        output.append("---@type ").append(type);
        if (optional) {
            output.append("|nil");
        }
        output.append(NEW_LINE);
    }

    static void writeObjDef(Writer output, String className) throws IOException {
        writeObjDef(output, className, false);
    }

    static void writeObjDef(Writer output, String className, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        output.append(className).append(" = {}").append(NEW_LINE).append(NEW_LINE);
    }

    static void writeClass(Writer output, String className, String parentClass) throws IOException {
        output.append("---@class ").append(className);
        if (!parentClass.isEmpty()) {
            output.append(" : ").append(parentClass);
        }
        output.append(NEW_LINE);
    }

    static void writeClass(Writer output, String className) throws IOException {
        writeClass(output, className, "");
    }

    static void writeClass(Writer output, String className, List<String> parentClasses) throws IOException {
        if (parentClasses != null && !parentClasses.isEmpty()) {
            writeClass(output, className, parentClasses.get(0));
        } else {
            writeClass(output, className);
        }
    }

    static void writeValDef(Writer output, String name, String parent, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        if (parent != null && !parent.isEmpty()) {
            if (name.contains("-")) {
                output.append(parent).append("[\"").append(name).append("\"]");
            } else {
                output.append(parent).append('.').append(name);
            }
        } else {
            output.append(name);
        }

        output.append(" = nil").append(NEW_LINE);
    }

    static void writeValDef(Writer output, String name, String parent) throws IOException {
        writeValDef(output, name, parent, false);
    }

    static void writeSee(Writer output, @Nullable List<String> seeAlso) throws IOException {
        if (seeAlso != null && !seeAlso.isEmpty()) {
            for (String see : seeAlso) {
                see = see.replace("::", "#");
                output.append("---@see ").append(see).append(NEW_LINE);
            }
        }
    }

    static <T> void writeAlias(Writer output, String name, @NotNull Stream<T> types, Function<T, String> function) throws IOException {
        List<String> list = types.map(function).collect(Collectors.toList());

        writeAlias(output, name, list);
    }

    static void writeAlias(Writer output, String name, @NotNull List<String> types) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();

        boolean first = true;
        for (String type : types) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('|');
            }
            stringBuilder.append(type);
        }

        writeAlias(output, name, stringBuilder.toString());
    }

    static void writeAlias(@NotNull Writer output, String name, String type) throws IOException {
        output.append("---@alias ").append(name).append(' ').append(type).append(NEW_LINE);
    }
}

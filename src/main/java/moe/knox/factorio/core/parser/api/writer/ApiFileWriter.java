package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.core.parser.api.data.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public final class ApiFileWriter
{
    private final static String NEW_LINE = System.lineSeparator();

    public void writeConcepts(OutputStreamWriter output, List<Concept> concepts) throws IOException {
        for (Concept concept : concepts) {
            switch (concept.category) {
                case "table": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeClass(output, concept.name);
                    writeObjDef(output, concept.name, true);
                    output.append(NEW_LINE);

                    for (Parameter parameter : concept.table.parameters) {
                        writeDescLine(output, parameter.description);
                        writeType(output, parameter.type, parameter.optional);
                        writeValDef(output, parameter.name, concept.name);
                        output.append(NEW_LINE);
                    }
                    break;
                }
                case "table_or_array": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeShape(output, concept.name);

                    int i = 1;
                    for (Parameter parameter : concept.tableOrArray.parameters) {
                        writeField(output, parameter.name, parameter.type, parameter.description);
                        writeField(output, "[" + i + "]", parameter.type, parameter.description);
                        ++i;
                    }

                    output.append(NEW_LINE);
                    break;
                }
                case "enum": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeClass(output, concept.name);

                    for (BasicMember option : concept._enum.options) {
                        String desc = "(Enum) " + option.description;
                        writeField(output, option.name, "number", desc);
                    }

                    output.append(NEW_LINE);
                    break;
                }
                case "flag": {
                    // define string-literal type
                    String aliasName = concept.name + "Value";
                    List<String> types = new ArrayList<>();
                    for (BasicMember option : concept.flag.options) {
                        types.add(option.name);
                    }
                    writeAliasStringLiteral(output, aliasName, types);
                    output.append(NEW_LINE);

                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);

                    // actual type is string-literal array
                    writeAlias(output, concept.name, aliasName + "[]");

                    output.append(NEW_LINE);
                    break;
                }
                case "union": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);

                    List<Type> types = new ArrayList<>();
                    for (Concept.CategoryUnion.Spec option : concept.union.options) {
                        types.add(option.type);
                        writeDescLine(output,getType(option.type) + ": " + option.description);
                    }
                    writeAlias(output, concept.name, types);
                    break;
                }
                case "filter": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeShape(output, concept.name);
                    writeObjDef(output, concept.name, true);

                    for (Parameter parameter : concept.filter.parameters) {
                        writeDescLine(output, parameter.description);
                        writeType(output, parameter.type, parameter.optional);
                        writeValDef(output, parameter.name, concept.name);
                        output.append(NEW_LINE);
                    }
                    break;
                }
                case "struct": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeClass(output, concept.name);
                    writeObjDef(output, concept.name, true);

                    writeAttributes(output, concept.struct.attributes, concept.name);
                    output.append(NEW_LINE);
                    break;
                }
            }
        }
    }

    public void writeGlobalsObjects(OutputStreamWriter output, List<GlobalObject> globalObjects) throws IOException {
        // global objects
        for (GlobalObject globalObject : globalObjects) {
            writeDescLine(output, globalObject.description);
            writeType(output, globalObject.type);
            writeObjDef(output, globalObject.name);
            output.append(NEW_LINE);
        }
        output.append(NEW_LINE);
    }

    public void writeDefines(OutputStreamWriter output, List<Define> defines, String parents) throws IOException {
        for (Define define : defines) {
            writeDescLine(output, define.description);

            StringWriter subDefine = new StringWriter();
            subDefine.append(parents).append('.').append(define.name);
            writeClass(output, subDefine.toString());
            writeObjDef(output, subDefine.toString());
            output.append(NEW_LINE);

            if (define.subkeys != null && !define.subkeys.isEmpty()) {
                writeDefines(output, define.subkeys, subDefine.toString());
            }
            if (define.values != null && !define.values.isEmpty()) {
                writeDefineValues(output, define.values, subDefine.toString());
            }
        }
        output.append(NEW_LINE);
    }

    public void writeClasses(OutputStreamWriter output, List<FactorioClass> classes) throws IOException {
        for (FactorioClass factorioClass : classes) {
            writeDescLine(output, factorioClass.description);
            writeDescLine(output, factorioClass.notes);
            writeDescLine(output, factorioClass.examples);
            writeSee(output, factorioClass.seeAlso);

            for (Operator operator : factorioClass.operators) {
                if (operator.name == "call") {
                    writeOverload(output, operator.method.parameters, operator.method.returnType);
                }
            }

            writeClass(output, factorioClass.name, factorioClass.baseClasses);
            writeObjDef(output, factorioClass.name, true);
            output.append(NEW_LINE);

            writeAttributes(output, factorioClass.attributes, factorioClass.name);
            writeMethods(output, factorioClass.methods, factorioClass.name);

            output.append(NEW_LINE);
        }
    }

    private void writeObjDef(OutputStreamWriter output, String className) throws IOException {
        writeObjDef(output, className, false);
    }

    private void writeObjDef(OutputStreamWriter output, String className, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        output.append(className).append(" = {}").append(NEW_LINE);
    }

    private void writeValDef(OutputStreamWriter output, String name) throws IOException {
        writeValDef(output, name, null, false);
    }

    private void writeValDef(OutputStreamWriter output, String name, String parent) throws IOException {
        writeValDef(output, name, parent, false);
    }

    private void writeValDef(OutputStreamWriter output, String name, boolean local) throws IOException {
        writeValDef(output, name, null, local);
    }

    private void writeValDef(OutputStreamWriter output, String name, String parent, boolean local) throws IOException {
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
            output.append(name).append(" = nil").append(NEW_LINE);
        }

        output.append(" = nil").append(NEW_LINE);
    }

    private void writeFunctionDef(OutputStreamWriter output, String className, String functionName, String... params) throws IOException {
        output.append("function ").append(className).append('.').append(functionName).append('(');
        boolean first = true;
        for (String param : params) {
            if (first) {
                first = false;
            } else {
                output.append(", ");
            }
            output.append(param);
        }
        output.append(") end").append(NEW_LINE);
    }

    private void writeDefineValues(OutputStreamWriter output, List<BasicMember> defines, String parents) throws IOException {
        for (BasicMember define : defines) {
            writeDescLine(output, define.description);
            writeType(output, "nil");
            writeValDef(output, define.name, parents);
            output.append(NEW_LINE);
        }
    }

    private void writeAttributes(OutputStreamWriter output, List<Attribute> attributes, String className) throws IOException {
        for (Attribute attribute : attributes) {
            writeDescLine(output, attribute.description);
            writeDescLine(output, attribute.notes);
            writeDescLine(output, attribute.examples);
            writeSee(output, attribute.seeAlso);
            writeReadWrite(output, attribute.read, attribute.write);
            writeType(output, attribute.type);
            writeValDef(output, attribute.name, className);
            output.append(NEW_LINE);
        }
    }

    private void writeMethods(OutputStreamWriter output, List<Method> methods, String className) throws IOException {
        for (Method method : methods) {
            writeDescLine(output, method.description);
            writeDescLine(output, method.notes);
            writeDescLine(output, method.examples);
            writeSee(output, method.seeAlso);

            if (method.takesTable) {
                // This is a table function (use anonymous function as only param)
                String paramType = TypeResolver.getAnonymousTableType(method.parameters);

                writeParam(output, "param", paramType);

                if (method.returnType != null) {
                    writeReturn(output, method.returnType, method.returnDescription);
                }

                writeFunctionDef(output, className, method.name, "param");
            } else {
                List<String> strList = new ArrayList<>();

                for (Parameter parameter : method.parameters) {
                    writeParam(output, parameter.name, parameter.type, parameter.description);

                    if (parameter.optional) {
                        writeOverload(output, method.parameters, method.returnType, parameter.name);
                    }

                    strList.add(parameter.name);
                }

                if (method.returnType != null) {
                    writeReturn(output, method.returnType, method.returnDescription);
                }

                writeFunctionDef(output, className, method.name, strList.toArray(new String[0]));
            }
            output.append(NEW_LINE);
        }
    }

    private void writeDescLine(OutputStreamWriter output, List<String> lines) throws IOException {
        if (lines != null && !lines.isEmpty()) {
            for (String line : lines) {
                writeEmptyLine(output);
                writeDescLine(output, line);
            }
        }
    }

    private void writeEmptyLine(OutputStreamWriter output) throws IOException {
        output.append("---").append(NEW_LINE);
    }

    private void writeDescLine(OutputStreamWriter output, String line) throws IOException {
        if (!line.isEmpty()) {
            line = line.replace('\n', ' ');
            output.append("--- ").append(line).append(NEW_LINE);
        }
    }

    private void writeReadWrite(OutputStreamWriter output, boolean read, boolean write) throws IOException {
        if (read && write) {
            output.append("--- ").append("Read-Write").append(NEW_LINE);
        } else if (read) {
            output.append("--- ").append("Read-Only").append(NEW_LINE);
        } else if (write) {
            output.append("--- ").append("Write-Only").append(NEW_LINE);
        }
    }

    private void writeSee(OutputStreamWriter output, List<String> seeAlso) throws IOException {
        if (seeAlso != null && !seeAlso.isEmpty()) {
            for (String see : seeAlso) {
                see = see.replace("::", "#");
                output.append("---@see ").append(see).append(NEW_LINE);
            }
        }
    }

    private void writeClass(OutputStreamWriter output, String className) throws IOException {
        writeClass(output, className, "");
    }

    private void writeClass(OutputStreamWriter output, String className, String parentClass) throws IOException {
        output.append("---@class ").append(className);
        if (!parentClass.isEmpty()) {
            output.append(" : ").append(parentClass);
        }
        output.append(NEW_LINE);
    }

    private void writeClass(OutputStreamWriter output, String className, List<String> parentClasses) throws IOException {
        if (parentClasses != null && !parentClasses.isEmpty()) {
            writeClass(output, className, parentClasses.get(0));
        } else {
            writeClass(output, className);
        }
    }

    private void writeShape(OutputStreamWriter output, String name) throws IOException {
        output.append("---@shape ").append(name).append(NEW_LINE);
    }

    private void writeField(OutputStreamWriter output, String name, Type type, String description) throws IOException {
        writeField(output, name, type, description, false);
    }

    private void writeField(OutputStreamWriter output, String name, String type, String description) throws IOException {
        writeField(output, name, type, description, false);
    }

    private void writeField(OutputStreamWriter output, String name, Type type, String description, boolean withNil) throws IOException {
        writeField(output, name, getType(type), description, withNil);
    }

    private void writeField(OutputStreamWriter output, String name, String type, String description, boolean withNil) throws IOException {
        output.append("---@field ").append(name).append(' ').append(type);

        if (withNil) {
            output.append("|nil");
        }

        output.append(' ').append(description);
    }

    private void writeType(OutputStreamWriter output, String type) throws IOException {
        writeType(output, type, false);
    }

    private void writeType(OutputStreamWriter output, Type type) throws IOException {
        writeType(output, getType(type), false);
    }

    private void writeType(OutputStreamWriter output, Type type, boolean optional) throws IOException {
        writeType(output, getType(type), optional);
    }

    private void writeType(OutputStreamWriter output, String type, boolean optional) throws IOException {
        output.append("---@type ").append(type);
        if (optional) {
            output.append("|nil");
        }
        output.append(NEW_LINE);
    }

    private void writeParam(OutputStreamWriter output, String name, Type type, String description) throws IOException {
        writeParam(output, name, getType(type), description);
    }

    private void writeParam(OutputStreamWriter output, String name, String type) throws IOException {
        writeParam(output, name, type, "");
    }

    private void writeParam(OutputStreamWriter output, String name, String type, String description) throws IOException {
        description = description.replace('\n', ' ');
        output.append("---@param ").append(name).append(' ').append(type).append(' ').append(description).append(NEW_LINE);
    }

    private void writeReturn(OutputStreamWriter output, Type type, String desc) throws IOException {
        output.append("---@return ").append(getType(type)).append(' ');
        if (!desc.isEmpty()) {
            desc = desc.replace('\n', ' ');
            output.append(desc);
        }
        output.append(NEW_LINE);
    }

    private void writeOverload(OutputStreamWriter output, List<Parameter> parameters, String stopAt) throws IOException {
        writeOverload(output, parameters, null, stopAt);
    }

    private void writeOverload(OutputStreamWriter output, List<Parameter> parameters, Type returnType) throws IOException {
        writeOverload(output, parameters, returnType, null);
    }

    private void writeOverload(OutputStreamWriter output, List<Parameter> parameters, Type returnType, String stopAt) throws IOException {
        // ---@overload fun(param1:A,param2:B):R

        output.append("---@overload fun(");

        boolean first = true;
        for (Parameter parameter : parameters) {
            if (stopAt != null && stopAt.equals(parameter.name)) {
                break;
            }

            if (first) {
                first = false;
            } else {
                output.append(',');
            }

            output.append(parameter.name).append(':').append(getType(parameter.type));
        }

        output.append(')');

        if (returnType != null) {
            output.append(':').append(getType(returnType));
        }

        output.append(NEW_LINE);
    }

    private void writeAliasStringLiteral(OutputStreamWriter output, String name, List<String> types) throws IOException {
        output.append("---@alias ").append(name);

        boolean first = true;
        for (String type : types) {
            if (first) {
                first = false;
            } else {
                output.append('|');
            }

            output.append('"').append(type).append('"');
        }
        output.append(NEW_LINE);
    }

    private void writeAlias(OutputStreamWriter output, String name, List<Type> types) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (Type type : types) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('|');
            }
            stringBuilder.append(getType(type));
        }
        writeAlias(output, name, stringBuilder.toString());
    }

    private void writeAlias(OutputStreamWriter output, String name, String type) throws IOException {
        output.append("---@alias ").append(name).append(' ').append(type).append(NEW_LINE);
    }

    private String getType(Type type)
    {
        return TypeResolver.getType(type);
    }
}
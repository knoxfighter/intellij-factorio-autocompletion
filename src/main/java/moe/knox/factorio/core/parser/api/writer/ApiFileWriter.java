package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public final class ApiFileWriter
{
    private final static String NEW_LINE = System.lineSeparator();
    private final Writer output;

    public ApiFileWriter(Writer output)
    {
        this.output = output;
    }

    public static ApiFileWriter fromIoWriter(Writer outputStreamWriter) {
        return new ApiFileWriter(outputStreamWriter);
    }

    public void writeRuntimeApi(RuntimeApi runtimeApi) throws IOException {
        writeGlobalsObjects(runtimeApi.globalObjects);

        writeDefines(runtimeApi.defines);

        // TODO: implement autocompletion for events

        writeClasses(runtimeApi.classes);

        writeConcepts(runtimeApi.concepts);
    }

    private void writeConcepts(List<Concept> concepts) throws IOException {
        writeHeaderBlock("Concepts");

        for (Concept concept : concepts) {
            if (concept.type() instanceof ValueType.Table) {
                writeConceptAsTable(concept);
            } else if (concept.type() instanceof ValueType.Union) {
                writeConceptAsUnion(concept);
            } else if (concept.type() instanceof ValueType.Simple) {
                writeConceptAsSimple(concept);
            } else if (concept.type() instanceof ValueType.Dictionary) {
                writeConceptAsDictionary(concept);
            } else if (
                concept.type() instanceof ValueType.Struct ||
                concept.type() instanceof ValueType.Tuple ||
                concept.type() instanceof ValueType.Array
            ) {
                // todo add realization
            } else {
                throw new IllegalArgumentException("Unknown concept type: " + concept.type().getNativeName());
            }
        }
    }

    private void writeGlobalsObjects(List<GlobalObject> globalObjects) throws IOException {
        writeHeaderBlock("Global objects");

        // global objects
        for (GlobalObject globalObject : globalObjects) {
            writeDescLine(output, globalObject.description);
            writeType(output, globalObject.type);
            writeObjDef(output, globalObject.name);
            output.append(NEW_LINE);
        }
        output.append(NEW_LINE);
    }

    private void writeDefines(List<Define> defines) throws IOException {
        writeHeaderBlock("Defines");

        output.append("---@class defines").append(NEW_LINE);
        output.append("defines = {}").append(NEW_LINE).append(NEW_LINE);
        writeDefines(defines, "defines");
    }

    private void writeDefines(List<Define> defines, String parents) throws IOException {
        writeHeaderBlock("Defines");

        for (Define define : defines) {
            writeDescLine(output, define.description);

            StringWriter subDefine = new StringWriter();
            subDefine.append(parents).append('.').append(define.name);
            writeClass(output, subDefine.toString());
            writeObjDef(output, subDefine.toString());
            output.append(NEW_LINE);

            if (define.subkeys != null && !define.subkeys.isEmpty()) {
                writeDefines(define.subkeys, subDefine.toString());
            }
            if (define.values != null && !define.values.isEmpty()) {
                writeDefineValues(output, define.values, subDefine.toString());
            }
        }
        output.append(NEW_LINE);
    }

    private void writeClasses(List<FactorioClass> classes) throws IOException {
        writeHeaderBlock("Classes");

        for (FactorioClass factorioClass : classes) {
            writeDescLine(output, factorioClass.description);
            writeDescLine(output, factorioClass.notes);
            writeDescLine(output, factorioClass.examples);
            writeSee(output, factorioClass.seeAlso);

            writeOperators(factorioClass);

            writeClass(output, factorioClass.name, factorioClass.baseClasses);
            writeObjDef(output, factorioClass.name, true);
            output.append(NEW_LINE);

            writeAttributes(factorioClass.attributes, factorioClass.name);
            writeMethods(output, factorioClass.methods, factorioClass.name);

            output.append(NEW_LINE);
        }
    }

    private void writeObjDef(Writer output, String className) throws IOException {
        writeObjDef(output, className, false);
    }

    private void writeObjDef(Writer output, String className, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        output.append(className).append(" = {}").append(NEW_LINE);
    }

    private void writeValDef(Writer output, String name, String parent) throws IOException {
        writeValDef(output, name, parent, false);
    }

    private void writeValDef(Writer output, String name, String parent, boolean local) throws IOException {
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

    private void writeFunctionDef(Writer output, String className, String functionName, String... params) throws IOException {
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

    private void writeDefineValues(Writer output, List<BasicMember> defines, String parents) throws IOException {
        for (BasicMember define : defines) {
            writeDescLine(output, define.description);
            writeType(output, "nil");
            writeValDef(output, define.name, parents);
            output.append(NEW_LINE);
        }
    }

    private void writeAttributes(List<Attribute> attributes, String className) throws IOException {
        for (Attribute attribute : attributes) {
            writeAttribute(attribute, className);
        }
    }

    private void writeAttribute(Attribute attribute, String className) throws IOException {
        writeDescLine(output, attribute.description);
        writeDescLine(output, attribute.notes);
        writeDescLine(output, attribute.examples);
        writeSee(output, attribute.seeAlso);
        writeReadWrite(output, attribute.read, attribute.write);
        writeType(output, attribute.type);
        writeValDef(output, attribute.name, className);
        output.append(NEW_LINE);
    }

    private void writeMethods(Writer output, List<Method> methods, String className) throws IOException {
        for (Method method : methods) {
            writeDescLine(output, method.description);
            writeDescLine(output, method.notes);
            writeDescLine(output, method.examples);
            writeSee(output, method.seeAlso);

            if (method.takesTable) {
                // This is a table function (use anonymous function as only param)
                String paramType = AnnotationTypeResolver.presentTableParams(method.parameters);

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

    private void writeDescLine(Writer output, List<String> lines) throws IOException {
        if (lines != null && !lines.isEmpty()) {
            for (String line : lines) {
                writeEmptyLine(output);
                writeDescLine(output, line);
            }
        }
    }

    private void writeEmptyLine(Writer output) throws IOException {
        output.append("---").append(NEW_LINE);
    }

    private void writeDescLine(Writer output, String line) throws IOException {
        if (!line.isEmpty()) {
            line = line.replace('\n', ' ');
            output.append("--- ").append(line).append(NEW_LINE);
        }
    }

    private void writeReadWrite(Writer output, boolean read, boolean write) throws IOException {
        if (read && write) {
            output.append("--- ").append("Read-Write").append(NEW_LINE);
        } else if (read) {
            output.append("--- ").append("Read-Only").append(NEW_LINE);
        } else if (write) {
            output.append("--- ").append("Write-Only").append(NEW_LINE);
        }
    }

    private void writeSee(Writer output, List<String> seeAlso) throws IOException {
        if (seeAlso != null && !seeAlso.isEmpty()) {
            for (String see : seeAlso) {
                see = see.replace("::", "#");
                output.append("---@see ").append(see).append(NEW_LINE);
            }
        }
    }

    private void writeClass(Writer output, String className) throws IOException {
        writeClass(output, className, "");
    }

    private void writeClass(Writer output, String className, String parentClass) throws IOException {
        output.append("---@class ").append(className);
        if (!parentClass.isEmpty()) {
            output.append(" : ").append(parentClass);
        }
        output.append(NEW_LINE);
    }

    private void writeClass(Writer output, String className, List<String> parentClasses) throws IOException {
        if (parentClasses != null && !parentClasses.isEmpty()) {
            writeClass(output, className, parentClasses.get(0));
        } else {
            writeClass(output, className);
        }
    }

    private void writeType(Writer output, String type) throws IOException {
        writeType(output, type, false);
    }

    private void writeType(Writer output, ValueType type) throws IOException {
        writeType(output, getType(type), false);
    }

    private void writeType(Writer output, ValueType type, boolean optional) throws IOException {
        writeType(output, getType(type), optional);
    }

    private void writeType(Writer output, String type, boolean optional) throws IOException {
        output.append("---@type ").append(type);
        if (optional) {
            output.append("|nil");
        }
        output.append(NEW_LINE);
    }

    private void writeParam(Writer output, String name, ValueType type, String description) throws IOException {
        writeParam(output, name, getType(type), description);
    }

    private void writeParam(Writer output, String name, String type) throws IOException {
        writeParam(output, name, type, "");
    }

    private void writeParam(Writer output, String name, String type, String description) throws IOException {
        description = description.replace('\n', ' ');
        output.append("---@param ").append(name).append(' ').append(type).append(' ').append(description).append(NEW_LINE);
    }

    private void writeReturn(Writer output, ValueType type, String desc) throws IOException {
        output.append("---@return ").append(getType(type)).append(' ');
        if (!desc.isEmpty()) {
            desc = desc.replace('\n', ' ');
            output.append(desc);
        }
        output.append(NEW_LINE);
    }

    private void writeOverload(Writer output, List<Parameter> parameters, ValueType returnType) throws IOException {
        writeOverload(output, parameters, returnType, null);
    }

    private void writeOverload(Writer output, List<Parameter> parameters, ValueType returnType, String stopAt) throws IOException {
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

    private void writeAlias(Writer output, String name, List<ValueType> types) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (ValueType type : types) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('|');
            }
            stringBuilder.append(getType(type));
        }
        writeAlias(output, name, stringBuilder.toString());
    }

    private void writeAlias(Writer output, String name, String type) throws IOException {
        output.append("---@alias ").append(name).append(' ').append(type).append(NEW_LINE);
    }

    private String getType(ValueType type)
    {
        return AnnotationTypeResolver.getType(type);
    }

    private void writeConceptAsTable(Concept concept) throws IOException {
        var conceptType = (ValueType.Table) concept.type();

        writeDescLine(output, concept.description());
        writeDescLine(output, concept.notes());
        writeDescLine(output, concept.examples());
        writeSee(output, concept.seeAlso());
        writeClass(output, concept.name());
        writeObjDef(output, concept.name(), true);
        output.append(NEW_LINE);

        for (Parameter parameter : conceptType.parameters()) {
            writeDescLine(output, parameter.description);
            writeType(output, parameter.type, parameter.optional);
            writeValDef(output, parameter.name, concept.name());
            output.append(NEW_LINE);
        }

        output.append(NEW_LINE);
    }

    private void writeConceptAsUnion(Concept concept) throws IOException {
        var conceptType = (ValueType.Union) concept.type();

        writeDescLine(output, concept.description());
        writeDescLine(output, concept.notes());
        writeDescLine(output, concept.examples());
        writeSee(output, concept.seeAlso());

        var types = new ArrayList<ValueType>();
        for (ValueType optionType : conceptType.options()) {
            types.add(optionType);
            writeDescLine(output, getType(optionType) + ": " + optionType.getDescription());
        }
        writeAlias(output, concept.name(), types);

        output.append(NEW_LINE);
    }

    private void writeConceptAsSimple(Concept concept) throws IOException {
        var conceptType = (ValueType.Simple) concept.type();

        writeDescLine(output, concept.description());
        writeDescLine(output, concept.notes());
        writeDescLine(output, concept.examples());
        writeSee(output, concept.seeAlso());
        writeAlias(output, concept.name(), conceptType.value());

        output.append(NEW_LINE);
    }

    private void writeConceptAsDictionary(Concept concept) throws IOException {
        var conceptType = (ValueType.Dictionary) concept.type();

        writeDescLine(output, concept.description());
        writeDescLine(output, concept.notes());
        writeDescLine(output, concept.examples());
        writeSee(output, concept.seeAlso());
        writeAlias(output, concept.name(), getType(conceptType));

        output.append(NEW_LINE);
    }

    private void writeHeaderBlock(@NotNull String blockName) throws IOException {
        output.append(NEW_LINE)
                .append("----------------------------------------------").append(NEW_LINE)
                .append("-".repeat(10)).append(" ").append(blockName).append(NEW_LINE)
                .append("----------------------------------------------").append(NEW_LINE)
                .append(NEW_LINE).append(NEW_LINE).append(NEW_LINE)
        ;
    }

    private void writeOperators(FactorioClass factorioClass) throws IOException {
        for (Operator operator : factorioClass.operators) {
            switch (operator.getType()) {
                case CALL -> writeOverload(output, operator.method.parameters, operator.method.returnType);
                case INDEX, LENGTH -> writeAttribute(operator.attribute, factorioClass.name);
            }
        }
    }
}

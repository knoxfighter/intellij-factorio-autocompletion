package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static moe.knox.factorio.core.parser.api.writer.TypeResolver.getType;
import static moe.knox.factorio.core.parser.api.writer.GeneralWriter.*;

public class ClassesWriter {
    /**
     * Output format:
     * <pre>
     * {@code
     * --- <class-description>
     * ---@class <class-name>
     * local <class-name> = {}
     *
     * --- <method-description>
     * ---@param <param-name> <param-type> <param-description>
     * ---@return <return-type> <return-description>
     * function <class-name>.<method-name>(<param-name>) end
     *
     * --- <attribute-description>
     * ---@type <attribute-type>
     * <class-name>.<attribute-name> = nil
     *
     * TODO: operators
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * ---<p> Entry point for registering event handlers. It is accessible through the global object named <code>script</code>. </p>
     * ---@class LuaBootstrap
     * LuaBootstrap = {}
     *
     * ---Register a handler to run every nth tick(s). When the game is on tick 0 it will trigger all registered handlers.
     * ---@param tick uint|uint[] The nth-tick(s) to invoke the handler on. Passing <code>nil</code> as the only parameter will unregister all nth-tick handlers.
     * ---@param f fun(nthTickEvent:NthTickEvent) The handler to run. Passing <code>nil</code> will unregister the handler for the provided ticks.
     * function LuaBootstrap.on_nth_tick(tick, f) end
     *
     * --- The name of the mod from the environment this is used in.
     * --- Read-Only
     * ---@type string
     * LuaBootstrap.mod_name = nil
     * }
     * </pre>
     */
    static void writeClasses(Writer output, @NotNull List<FactorioClass> classes) throws IOException {
        writeHeaderBlock(output, "Classes");

        for (FactorioClass factorioClass : classes) {
            writeDescLine(output, factorioClass.description);
            writeDescLine(output, factorioClass.notes);
            writeDescLine(output, factorioClass.examples);
            writeSee(output, factorioClass.seeAlso);

            writeClass(output, factorioClass.name, factorioClass.baseClasses);
            writeObjDef(output, factorioClass.name, true);

            writeOperators(output, factorioClass);

            writeAttributes(output, factorioClass.attributes, factorioClass.name);
            writeMethods(output, factorioClass.methods, factorioClass.name);

            output.append(NEW_LINE);
        }
    }

    private static void writeOperators(Writer output, @NotNull FactorioClass factorioClass) throws IOException {
        for (Operator operator : factorioClass.operators) {
            if (operator instanceof Operator.Attribute) {
                // TODO write attribute, those are not "normal" attributes, but overridden LUA operators: `#` and `[]`
                // writeAttribute(((Attribute) operator), factorioClass.name);
            } else if (operator instanceof Operator.Method) {
                // TODO write method, that is the operator `()`, which results in this type beeing able to be called directly (as though it is a function)
                // case CALL -> writeOverload(output, operator.method.parameters, operator.method.returnType);
            }
        }
    }

    static void writeAttributes(Writer output, @NotNull List<Attribute> attributes, String className) throws IOException {
        for (Attribute attribute : attributes) {
            writeAttribute(output, attribute, className);
        }
    }

    /**
     * Output format:
     * <pre>
     * {@code
     * --- <attribute-description>
     * ---@type <attribute-type>
     * <class-name>.<attribute-name> = nil
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * --- The name of the mod from the environment this is used in.
     * --- Read-Only
     * ---@type string
     * LuaBootstrap.mod_name = nil
     * }
     * </pre>
     */
    private static void writeAttribute(Writer output, @NotNull Attribute attribute, String className) throws IOException {
        writeDescLine(output, attribute.description);
        writeDescLine(output, attribute.notes);
        writeDescLine(output, attribute.examples);
        writeSee(output, attribute.seeAlso);
        writeReadWrite(output, attribute.read, attribute.write);
        writeType(output, attribute.type);
        writeValDef(output, attribute.name, className);
        output.append(NEW_LINE);
    }

    private static void writeReadWrite(Writer output, boolean read, boolean write) throws IOException {
        if (read && write) {
            output.append("--- ").append("Read-Write").append(NEW_LINE);
        } else if (read) {
            output.append("--- ").append("Read-Only").append(NEW_LINE);
        } else if (write) {
            output.append("--- ").append("Write-Only").append(NEW_LINE);
        }
    }

    /**
     * Output format:
     * <pre>
     * {@code
     * --- <method-description>
     * ---@param <param-name> <param-type> <param-description>
     * ---@return <return-type> <return-description>
     * function <class-name>.<method-name>(<param-name>) end
     * }
     * </pre>
     *
     * Example:
     * <pre>
     * {@code
     * ---Register a handler to run every nth tick(s). When the game is on tick 0 it will trigger all registered handlers.
     * ---@param tick uint|uint[] The nth-tick(s) to invoke the handler on. Passing <code>nil</code> as the only parameter will unregister all nth-tick handlers.
     * ---@param f fun(nthTickEvent:NthTickEvent) The handler to run. Passing <code>nil</code> will unregister the handler for the provided ticks.
     * function LuaBootstrap.on_nth_tick(tick, f) end
     * }
     * </pre>
     */
    private static void writeMethods(Writer output, List<Method> methods, String className) throws IOException {
        for (Method method : methods) {
            writeDescLine(output, method.description);
            writeDescLine(output, method.notes);
            writeDescLine(output, method.examples);
            writeSee(output, method.seeAlso);

            if (method.takesTable) {
                // This is a table function (use anonymous function as only param)
                String paramType = TypeResolver.presentTableParams(method.parameters);

                writeParam(output, "param", paramType);

                if (method.returnValues != null && !method.returnValues.isEmpty()) {
                    writeReturnValues(output, method.returnValues);
                } else if (method.returnType != null) {
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

                if (method.returnValues != null && !method.returnValues.isEmpty()) {
                    writeReturnValues(output, method.returnValues);
                } else if (method.returnType != null) {
                    writeReturn(output, method.returnType, method.returnDescription);
                }

                writeFunctionDef(output, className, method.name, strList.toArray(new String[0]));
            }
            output.append(NEW_LINE);
        }
    }

    private static void writeParam(Writer output, String name, ValueType type, String description) throws IOException {
        writeParam(output, name, getType(type), description);
    }

    private static void writeParam(Writer output, String name, String type) throws IOException {
        writeParam(output, name, type, "");
    }

    private static void writeParam(@NotNull Writer output, String name, String type, String description) throws IOException {
        description = description.replace('\n', ' ');
        output.append("---@param ").append(name).append(' ').append(type).append(' ').append(description).append(NEW_LINE);
    }

    /**
     * Output Format:
     * <pre>
     * {@code
     * ---@return <return1>, <return2>[, ...]
     * }
     * </pre>
     */
    private static void writeReturnValues(@NotNull Writer output, @NotNull List<Parameter> returnValues) throws IOException {
        output.append("---@return ");

        boolean first = true;
        for (Parameter returnValue : returnValues) {
            if (!first) {
                output.append(", ");
            }

            output.append(getType(returnValue.type));

            first = false;
        }
        output.append(NEW_LINE);
    }

    /**
     * Output Format:
     * <pre>
     * {@code
     * ---@return <type> <description>
     * }
     * </pre>
     */
    private static void writeReturn(@NotNull Writer output, ValueType type, @NotNull String desc) throws IOException {
        output.append("---@return ").append(getType(type)).append(' ');
        if (!desc.isEmpty()) {
            desc = desc.replace('\n', ' ');
            output.append(desc);
        }
        output.append(NEW_LINE);
    }

    private static void writeOverload(Writer output, List<Parameter> parameters, ValueType returnType) throws IOException {
        writeOverload(output, parameters, returnType, null);
    }

    /**
     * Output Format:
     * <pre>
     * {@code
     * ---@overload fun(<param1.name>:<param1.type>, <param2.name>:<param2.type>[, ...]):<return-type>
     * }
     * </pre>
     *
     * @param stopAt the name of the parameter to stop at, this allows to easily add optional params as overloads
     */
    private static void writeOverload(@NotNull Writer output, @NotNull List<Parameter> parameters, ValueType returnType, String stopAt) throws IOException {
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

    private static void writeFunctionDef(@NotNull Writer output, String className, String functionName, String @NotNull ... params) throws IOException {
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
}

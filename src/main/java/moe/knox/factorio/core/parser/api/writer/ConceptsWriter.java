package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.Concept;
import moe.knox.factorio.api.parser.data.Parameter;
import moe.knox.factorio.api.parser.data.ValueType;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static moe.knox.factorio.core.parser.api.writer.ClassesWriter.writeAttributes;
import static moe.knox.factorio.core.parser.api.writer.GeneralWriter.*;
import static moe.knox.factorio.core.parser.api.writer.TypeResolver.getType;

// TODO: EmmyLUA does not support typed tuples
// When we switch to Luanalysis, change this to a strongly typed tuple:
// https://github.com/Benjamin-Dobell/IntelliJ-Luanalysis#strongly-typed-tuples

public class ConceptsWriter {
    static void writeConcepts(Writer output, @NotNull List<Concept> concepts) throws IOException {
        writeHeaderBlock(output, "Concepts");

        for (Concept concept : concepts) {
            writeDesc(output, concept);

            if (concept instanceof Concept.Table) {
                writeOldTable(output, (Concept.Table) concept);
            } else if (concept instanceof Concept.TableOrArray) {
                writeOldTableOrArray(output, (Concept.TableOrArray) concept);
            } else if (concept instanceof Concept.Enum) {
                writeOldEnum(output, (Concept.Enum) concept);
            } else if (concept instanceof Concept.Flag) {
                writeOldFlag(output, (Concept.Flag) concept);
            } else if (concept instanceof Concept.Union) {
                writeOldUnion(output, (Concept.Union) concept);
            } else if (concept instanceof Concept.Struct) {
                writeOldStruct(output, (Concept.Struct) concept);
            } else {
                writeModern(output, concept);
            }
        }
    }

    private static void writeDesc(Writer output, @NotNull Concept concept) throws IOException {
        writeDescLine(output, concept.description);
        writeDescLine(output, concept.notes);
        writeDescLine(output, concept.examples);
        writeSee(output, concept.seeAlso);
    }

    private static void writeObjDef(Writer output, @NotNull Concept concept) throws IOException {
        writeClass(output, concept.name);
        GeneralWriter.writeObjDef(output, concept.name, true);
    }

    /**
     * "parameters" are not parameters as it seems.
     * Those are attributes.
     * Concepts and additional types use `parameters` as `attributes`!
     */
    static void writeParameter(Writer output, @NotNull List<Parameter> parameters, String parent) throws IOException {
        for (Parameter parameter : parameters) {
            writeDescLine(output, parameter.description);
            writeType(output, parameter.type, parameter.optional);
            writeValDef(output, parameter.name, parent);
            output.append(NEW_LINE);
        }
    }

    private static void writeOldTable(Writer output, @NotNull Concept.Table concept) throws IOException {
        writeObjDef(output, concept);

        writeParameter(output, concept.parameters, concept.name);

        // TODO add variant parameter group

        output.append(NEW_LINE);
    }

    /**
     * This type can be a table or a tuple (aka array)
     * TODO: EmmyLUA does not support typed tuples, so ignore them
     */
    private static void writeOldTableOrArray(Writer output, @NotNull Concept.TableOrArray concept) throws IOException {
        writeObjDef(output, concept);

        writeParameter(output, concept.parameters, concept.name);

        output.append(NEW_LINE);
    }

    private static void writeOldEnum(Writer output, @NotNull Concept.Enum concept) throws IOException {
        writeAlias(output, concept.name, concept.options.stream(), basicMember -> basicMember.name);

        output.append(NEW_LINE);
    }


    private static void writeOldFlag(Writer output, @NotNull Concept.Flag concept) throws IOException {
        String subName = concept.name + "Values";
        writeAlias(output, subName, concept.options.stream(), member -> member.name);

        output.append(NEW_LINE);

        writeAlias(output, concept.name, subName + "[]");

        output.append(NEW_LINE);
    }

    private static void writeOldUnion(Writer output, @NotNull Concept.Union concept) throws IOException {
        var types = new ArrayList<ValueType>();
        for (Concept.Union.Spec optionType : concept.options) {
            types.add(optionType.type);
            if (optionType.description != null) {
                writeDescLine(output, getType(optionType.type) + ": " + optionType.description);
            }
        }
        writeAlias(output, concept.name, types.stream(), valueType -> getType(valueType));

        output.append(NEW_LINE);
    }

    private static void writeOldStruct(Writer output, @NotNull Concept.Struct concept) throws IOException {
        writeObjDef(output, concept);

        writeAttributes(output, concept.attributes, concept.name);

        output.append(NEW_LINE);
    }

    private static void writeModern(Writer output, @NotNull Concept concept) throws IOException {
        if (concept.type instanceof ValueType.Table) {
            writeModernTable(output, concept);
        } else if (concept.type instanceof ValueType.Tuple) {
            writeModernTuple(output, concept);
        } else if (concept.type instanceof ValueType.Struct) {
            writeModernStruct(output, concept);
        } else if (concept.type instanceof ValueType.Array) {
            writeModernAlias(output, concept);
        } else if (concept.type instanceof ValueType.Dictionary) {
            writeModernAlias(output, concept);
        } else if (concept.type instanceof ValueType.Variant) {
            writeModernAlias(output, concept);
        } else if (concept.type instanceof ValueType.Simple) {
            writeModernAlias(output, concept);
        }
    }

    private static void writeModernTable(Writer output, Concept concept) throws IOException {
        writeObjDef(output, concept);

        ValueType.Table type = (ValueType.Table) concept.type;

        writeParameter(output, type.parameters, concept.name);

        // TODO add variant parameter group

        output.append(NEW_LINE);
    }

    /**
     * EmmyLUA does not support explicit tuples, so this does nothing.
     */
    private static void writeModernTuple(Writer output, Concept concept) throws IOException {
        // TODO: Tuples not supported in EmmyLUA.
    }

    private static void writeModernStruct(Writer output, @NotNull Concept concept) throws IOException {
        writeObjDef(output, concept);

        ValueType.Struct type = (ValueType.Struct) concept.type;
        writeAttributes(output, type.attributes(), concept.name);

        output.append(NEW_LINE);
    }

    private static void writeModernAlias(Writer output, Concept concept) throws IOException {
        writeAlias(output, concept.name, getType(concept.type));

        output.append(NEW_LINE);
    }
}

package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.*;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


// TODO: Add variadic parameters back in!

public final class ApiFileWriter
{
    private final Writer output;

    public ApiFileWriter(Writer output)
    {
        this.output = output;
    }

    public static ApiFileWriter fromIoWriter(Writer outputStreamWriter) {
        return new ApiFileWriter(outputStreamWriter);
    }

    public void writeRuntimeApi(RuntimeApi runtimeApi) throws IOException {
        GlobalObjectsWriter.writeGlobalsObjects(output, runtimeApi.globalObjects);

        DefinesWriter.writeDefines(output, runtimeApi.defines);

        // TODO: implement autocompletion for events

        ClassesWriter.writeClasses(output, runtimeApi.classes);

        ConceptsWriter.writeConcepts(output, runtimeApi.concepts);

        AdditionalTypesWriter.writeAdditionalTypes(output);
    }
}

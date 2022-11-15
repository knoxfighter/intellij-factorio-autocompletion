package moe.knox.factorio.core.parser.api.writer;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static moe.knox.factorio.core.parser.api.writer.GeneralWriter.*;

public class AdditionalTypesWriter {
    static void writeAdditionalTypes(Writer output) throws IOException {
        writeHeaderBlock(output, "Additional Types");

        do {
            List<TypeResolver.AdditionalType> additionalTypes = new ArrayList<>();
            additionalTypes.addAll(TypeResolver.additionalTypes);
            TypeResolver.additionalTypes.clear();
            writeAdditionalTypes(output, additionalTypes);
        } while (!TypeResolver.additionalTypes.isEmpty());
    }

    private static void writeAdditionalTypes(Writer output, List<TypeResolver.AdditionalType> additionalTypes) throws IOException {
        for (TypeResolver.AdditionalType additionalType : additionalTypes) {
            writeClass(output, additionalType.name);
            writeObjDef(output, additionalType.name);

            ConceptsWriter.writeParameter(output, additionalType.parameters, additionalType.name);
        }
    }
}

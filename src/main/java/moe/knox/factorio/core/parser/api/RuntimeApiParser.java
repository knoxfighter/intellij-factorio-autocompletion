package moe.knox.factorio.core.parser.api;

import com.google.gson.GsonBuilder;
import moe.knox.factorio.core.CoreException;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.core.parser.api.data.RuntimeApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public final class RuntimeApiParser {
    private final String factorioApiBaseLink = "https://lua-api.factorio.com";

    public RuntimeApi parse(FactorioVersion version) throws CoreException {
        try (InputStreamReader inputStreamReader = new InputStreamReader(createVersionStream(version))) {
            GsonBuilder builder = new GsonBuilder();
            ParsingHelper.addDeserializers(builder);
            RuntimeApi runtimeApi = builder
                    .create()
//                    .registerTypeAdapter(Concept.class, new JsonPolymorphismDeserializer<Concept>())
//                    .registerTypeAdapter(Type.ComplexData.class, new JsonPolymorphismDeserializer<Type.ComplexData>())
//                    .registerTypeAdapter(Operator.class, new JsonPolymorphismDeserializer<Operator>())
                    .fromJson(inputStreamReader, RuntimeApi.class);
            runtimeApi.arrangeElements();

            return runtimeApi;
        } catch (IOException e) {
            throw new CoreException("Parsing error", e);
        }
    }

    private InputStream createVersionStream(FactorioVersion version) throws IOException {
        String url = factorioApiBaseLink + "/" + version.version() + "/runtime-api.json";

        return (new URL(url)).openStream();
    }
}

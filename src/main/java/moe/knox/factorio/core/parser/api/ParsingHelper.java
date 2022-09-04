package moe.knox.factorio.core.parser.api;

import com.google.gson.*;
import marcono1234.gson.recordadapter.RecordTypeAdapterFactory;
import moe.knox.factorio.core.parser.api.data.*;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphismDeserializer;

public class ParsingHelper {
    public static GsonBuilder addDeserializers(GsonBuilder builder) {
        return addDeserializers(builder, null);
    }

    public static GsonBuilder addDeserializers(GsonBuilder builder, Class excludeType) {
        builder.registerTypeAdapterFactory(RecordTypeAdapterFactory.builder().allowMissingComponentValues().create());

        if (excludeType == null || excludeType != Operator.class) {
            builder.registerTypeAdapter(Operator.class, new JsonPolymorphismDeserializer<Operator>());
        }

        if (excludeType == null || excludeType != Parameter.class) {
            builder.registerTypeAdapter(Parameter.class, new Parameter.ParameterDeserializer());
        }

        return builder;
    }
}

package moe.knox.factorio.core.parser.api.data;

import com.google.gson.GsonBuilder;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphismDeserializer;

public class Helper {
    static GsonBuilder addDeserializers(GsonBuilder builder) {
        return addDeserializers(builder, null);
    }

    static GsonBuilder addDeserializers(GsonBuilder builder, Class excludeType) {
        if (excludeType == null || excludeType != Concept.class) {
            builder.registerTypeAdapter(Concept.class, new JsonPolymorphismDeserializer<Concept>());
        }

        if (excludeType == null || excludeType != Type.ComplexData.class) {
            builder.registerTypeAdapter(Type.ComplexData.class, new JsonPolymorphismDeserializer<Type.ComplexData>());
        }

        if (excludeType == null || excludeType != Operator.class) {
            builder.registerTypeAdapter(Operator.class, new JsonPolymorphismDeserializer<Operator>());
        }

        if (excludeType == null || excludeType != Parameter.class) {
            builder.registerTypeAdapter(Parameter.class, new Parameter.ParameterDeserializer());
        }

        return builder;
    }
}

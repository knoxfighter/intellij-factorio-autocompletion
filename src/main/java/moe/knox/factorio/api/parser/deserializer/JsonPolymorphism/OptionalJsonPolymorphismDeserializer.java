package moe.knox.factorio.api.parser.deserializer.JsonPolymorphism;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import moe.knox.factorio.api.parser.deserializer.Helper;
import moe.knox.factorio.api.parser.helper.ParsingHelper;

import java.io.IOException;

/**
 * This does exactly the same as {@link JsonPolymorphismDeserializer}.
 * It has only one difference: With this class the "type" field in the json is optional.
 * If that field is not there, the baseclass itself will be further deserialized.
 */
public class OptionalJsonPolymorphismDeserializer implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                JsonElement jsonElement = JsonParser.parseReader(in);

                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    JsonPolymorphismType annotation = type.getRawType().getAnnotation(JsonPolymorphismType.class);
                    if (annotation != null) {
                        String typeFieldName = annotation.value();

                        if (jsonObject.has(typeFieldName)) {
                            String typeName = jsonObject.get(typeFieldName).getAsString();

                            Class<T> clazz = Helper.getAnnotatedClassForType(typeName, type);

                            if (clazz != null) {
                                return ParsingHelper.getBuilder().create().fromJson(jsonElement, clazz);
                            }
                        }
                    }
                }

                return delegate.fromJsonTree(jsonElement);
            }
        };
    }
}

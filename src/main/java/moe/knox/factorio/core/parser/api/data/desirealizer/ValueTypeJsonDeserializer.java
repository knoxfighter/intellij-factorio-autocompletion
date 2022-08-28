package moe.knox.factorio.core.parser.api.data.desirealizer;

import com.google.gson.*;
import moe.knox.factorio.core.parser.api.ParsingHelper;
import moe.knox.factorio.core.parser.api.data.ValueType;
import org.jetbrains.annotations.NotNull;

public class ValueTypeJsonDeserializer implements JsonDeserializer<ValueType>
{
    @Override
    public ValueType deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonPrimitive()) {
            return new ValueType.Simple(jsonElement.getAsString());
        }

        if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("complex_type")) {
            return deserializeComplexType(jsonElement);
        }

        return null;
    }

    private ValueType deserializeComplexType(JsonElement jsonElement)
    {
        var jsonObject = jsonElement.getAsJsonObject();
        var complexTypeNativeName = jsonObject.get("complex_type").getAsString();
        var clazz = getTypeClass(complexTypeNativeName);
        var builder = (new GsonBuilder());

        ParsingHelper.addDeserializers(builder);

        return (builder.create()).fromJson(jsonElement, clazz);
    }

    private Class<? extends ValueType> getTypeClass(@NotNull String complexTypeNativeName) {
        Class<? extends ValueType> clazz = ValueType.TYPES_PER_NATIVE_NAME.get(complexTypeNativeName);

        if (clazz == null) {
            throw new RuntimeException("Unknown complex type");
        }

        return clazz;
    }
}

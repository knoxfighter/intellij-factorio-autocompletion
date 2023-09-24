package moe.knox.factorio.core.parser.api.data.desirealizer;

import com.google.gson.*;
import com.intellij.openapi.diagnostic.Logger;
import moe.knox.factorio.core.parser.api.ParsingHelper;
import moe.knox.factorio.core.parser.api.data.ValueType;
import org.jetbrains.annotations.NotNull;

public class ValueTypeJsonDeserializer implements JsonDeserializer<ValueType>
{
    private static final Logger logger = Logger.getInstance(ValueTypeJsonDeserializer.class);

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
        try {
            var jsonObject = jsonElement.getAsJsonObject();
            var complexTypeNativeName = jsonObject.get("complex_type").getAsString();
            var clazz = getTypeClass(complexTypeNativeName);
            var builder = (new GsonBuilder());

            ParsingHelper.addDeserializers(builder);

            return (builder.create()).fromJson(jsonElement, clazz);
        } catch (UnknownComplexTypeException e) {
            logger.warn("Unknown value type: " + e.getValueType());
            return new ValueType.Unknown();
        }
    }

    private Class<? extends ValueType> getTypeClass(@NotNull String complexTypeNativeName) {
        Class<? extends ValueType> clazz = ValueType.TYPES_PER_NATIVE_NAME.get(complexTypeNativeName);

        if (clazz == null) {
            throw new UnknownComplexTypeException(complexTypeNativeName);
        }

        return clazz;
    }
}

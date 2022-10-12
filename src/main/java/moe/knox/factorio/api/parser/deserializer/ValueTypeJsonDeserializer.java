package moe.knox.factorio.api.parser.deserializer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import moe.knox.factorio.api.parser.data.ValueType;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismDeserializer;

import java.lang.reflect.Type;

/**
 * Specialization to allow this class to be only a simple string.
 * Will run the normal {@link JsonPolymorphismDeserializer} logic otherwise.
 */
public class ValueTypeJsonDeserializer extends JsonPolymorphismDeserializer<ValueType> {
    @Override
    public ValueType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json.isJsonPrimitive()) {
            return new ValueType.Simple(json.getAsString());
        }
        if (json.isJsonObject()) {
            return super.deserialize(json, typeOfT, context);
        }

        return null;
    }
}

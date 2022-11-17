package moe.knox.factorio.api.parser.deserializer;

import com.google.gson.*;
import moe.knox.factorio.api.parser.data.ValueType;

import java.lang.reflect.Type;

/**
 * Custom deserializer to a {@link ValueType.Literal}.
 * This reads the "description" field as is and the "value" field as either primitive.
 */
public class ValueTypeLiteralDeserializer implements JsonDeserializer<ValueType.Literal> {
    @Override
    public ValueType.Literal deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Object literalValue = null;
        String description = "";

        if (json.isJsonObject()) {
            JsonObject object = json.getAsJsonObject();

            if (object.has("value")) {
                JsonElement value = object.get("value");
                if (value.isJsonPrimitive()) {
                    JsonPrimitive primitive = value.getAsJsonPrimitive();
                    if (primitive.isNumber()) {
                        literalValue = primitive.getAsDouble();
                    } else if (primitive.isBoolean()) {
                        literalValue = primitive.getAsBoolean();
                    } else if (primitive.isString()) {
                        literalValue = primitive.getAsString();
                    }
                }
            }
            if (object.has("description")) {
                JsonElement jsonDesc = object.get("description");
                description = jsonDesc.getAsString();
            }
        }

        return new ValueType.Literal(literalValue, description);
    }
}

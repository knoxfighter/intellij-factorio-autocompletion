package moe.knox.factorio.prototypeDefinition.types;

import com.google.gson.*;

import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Target(ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@interface PrototypeValue {
    String[] value();
}

public class Prototype {
    public String name;
    public String type;
    String link;
    String description;

    @PrototypeValue("table")
    public transient Table table = new Table();

    @PrototypeValue("alias")
    public transient String alias;

    @PrototypeValue({"prototype", "string", "stringArray"})
    public transient Map<String, String> stringMap = new HashMap<>();

    private static Field getAnnotatedFieldForType(String type) {
        // iterate over all fields in Prototype class
        for (Field field : Prototype.class.getFields()) {
            // get my annotation
            PrototypeValue annotation = field.getAnnotation(PrototypeValue.class);
            // only run if annotation was found
            if (annotation != null) {
                String[] types = annotation.value();
                // check if this annotation defines, that we want to use this field for data
                if (Arrays.stream(types).anyMatch(type::equals)) {
                    return field;
                }
            }
        }
        return null;
    }

    public static class PrototypeDeserializer implements JsonDeserializer<Prototype> {
        @Override
        public Prototype deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            // deserialize annotated fields
            Prototype prototype = new Gson().fromJson(json, Prototype.class);

            // get full json object
            JsonObject jsonObject = json.getAsJsonObject();

            // check if this object has a "value" field, where the information is saved
            if (jsonObject.has("value")) {
                JsonElement jsonValue = jsonObject.get("value");

                if (jsonValue != null && !jsonValue.isJsonNull()) {
                    Field field = Prototype.getAnnotatedFieldForType(prototype.type);

                    if (field != null) {
                        // get type of the field
                        Class<?> fieldType = field.getType();

                        Object fieldFromJson = new Gson().fromJson(jsonValue, fieldType);
                        try {
                            // set the field on the prototype object
                            field.set(prototype, fieldFromJson);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                            throw new JsonParseException(e);
                        }
                    }
                }
            }
            return prototype;
        }
    }

    public static class PrototypeSerializer implements JsonSerializer<Prototype> {
        @Override
        public JsonElement serialize(Prototype src, Type typeOfSrc, JsonSerializationContext context) {
            // serialize annotated fields
            JsonElement jsonElement = new Gson().toJsonTree(src, typeOfSrc);

            // get overallElement as jsonObject
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            if (src.type != null) {
                Field field = Prototype.getAnnotatedFieldForType(src.type);

                if (field != null) {
                    try {
                        Object valueField = field.get(src);
                        JsonElement valueJson = new Gson().toJsonTree(valueField, field.getGenericType());
                        jsonObject.add("value", valueJson);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        throw new JsonParseException(e);
                    }
                }
            }
            return jsonElement;
        }
    }
}

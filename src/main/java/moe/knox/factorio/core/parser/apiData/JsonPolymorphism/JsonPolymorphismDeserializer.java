package moe.knox.factorio.core.parser.apiData.JsonPolymorphism;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;

public class JsonPolymorphismDeserializer<T> implements JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        T res = new Gson().fromJson(jsonElement, type);

        JsonPolymorphismClass classAnnotation = TypeToken.get(type).getRawType().getAnnotation(JsonPolymorphismClass.class);
        String srcFieldName = classAnnotation.value();

        Field srcField;
        try {
            srcField = TypeToken.get(type).getRawType().getDeclaredField(srcFieldName);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return res;
        }

        if (!srcField.getType().isAssignableFrom(String.class)) {
            return res;
        }

        srcField.setAccessible(true);

        Object srcFieldVal;
        try {
            srcFieldVal = srcField.get(res);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return res;
        }

        Field field = getAnnotatedFieldForType((String)srcFieldVal, type);

        if (field == null) {
            return res;
        }

        Object data = new Gson().fromJson(jsonElement, field.getType());

        field.setAccessible(true);
        try {
            field.set(res, data);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return res;
        }

        return res;
    }

    private Field getAnnotatedFieldForType(String val, Type typeOf) {
        // iterate over all fields in Prototype class
        for (Field field : TypeToken.get(typeOf).getRawType().getDeclaredFields()) {
            // get my annotation
            JsonPolymorphism annotation = field.getAnnotation(JsonPolymorphism.class);
            // only run if annotation was found
            if (annotation != null) {
                String[] values = annotation.value();
                // check if this annotation defines, that we want to use this field for data
                if (Arrays.stream(values).anyMatch(val::equals)) {
                    return field;
                }
            }
        }
        return null;
    }
}

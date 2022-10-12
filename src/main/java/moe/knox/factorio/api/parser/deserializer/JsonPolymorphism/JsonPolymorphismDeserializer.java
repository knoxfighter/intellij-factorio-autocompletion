package moe.knox.factorio.api.parser.deserializer.JsonPolymorphism;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import moe.knox.factorio.api.parser.deserializer.Helper;
import moe.knox.factorio.api.parser.helper.ParsingHelper;

import java.lang.reflect.Type;

/**
 * Deserializer to allow Polymorph json objects.
 * <br>
 * Example json files. The type field of the "some" object defines which other fields the object contains.
 *
 * <pre>
 * {@code
 * {
 *     "some": {
 *         "type": "str",
 *         "value": "i am a string"
 *     }
 * }
 * }
 * </pre>
 *
 * <pre>
 * {@code
 * {
 *     "some": {
 *         "type": "description",
 *         "description": "i am the description"
 *     }
 * }
 * }
 * </pre>
 *
 * Example java classes to read that json layout:
 * <pre>
 * {@code
 * // For easy registering the class to gson.
 * @JsonAdapter(JsonPolymorphismDeserializer.class)
 * @JsonPolymorphismType("type")
 * interface SomeType {
 *     @JsonPolymorphismValue("str")
 *     class Str implements SomeType {
 *         public String value;
 *     }
 *
 *     @JsonPolymorphismValue("description")
 *     class Description implements SomeType {
 *         public String description;
 *     }
 * }
 *
 * class JsonObj {
 *     SomeType some;
 * }
 * }
 * </pre>
 *
 * With that code you can now call Gson to deserialize either of the upper Jsons.
 * <pre>
 * {@code
 * JsonObj obj = new Gson().fromJson(json1, JsonObj.class);
 * // obj.some instanceof SomeType.Str -> true
 *
 * JsonObj obj = new Gson().fromJson(json2, JsonObj.class);
 * // obj.some instanceof SomeType.Description -> true
 * }
 * </pre>
 */
public class JsonPolymorphismDeserializer<T> implements JsonDeserializer<T> {
    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) return null;

        String typeFieldName = TypeToken.get(typeOfT).getRawType().getAnnotation(JsonPolymorphismType.class).value();

        String typeName = json.getAsJsonObject().get(typeFieldName).getAsString();

        Class<T> clazz = Helper.getAnnotatedClassForType(typeName, ((TypeToken<T>) TypeToken.get(typeOfT)));

        GsonBuilder gsonBuilder = ParsingHelper.getBuilder();
        T obj = gsonBuilder.create().fromJson(json, clazz);

        return obj;
    }
}

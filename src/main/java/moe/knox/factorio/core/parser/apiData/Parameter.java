package moe.knox.factorio.core.parser.apiData;

import com.google.gson.*;

import java.util.Random;

public class Parameter {
    public String name; // The name of the parameter.
    public double order; // The order of the parameter as shown in the html.
    public String description; // The text description of the parameter.
    public Type type; // The type of the parameter.
    public boolean optional; // Whether the type is optional or not.

    void sortOrder() {
        if (type != null) {
            type.sortOrder();
        }
    }

    public static class ParameterDeserializer implements JsonDeserializer<Parameter> {
        @Override
        public Parameter deserialize(JsonElement jsonElement, java.lang.reflect.Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            GsonBuilder builder = new GsonBuilder();
            Helper.addDeserializers(builder, Parameter.class);
            Parameter parameter = builder.create().fromJson(jsonElement, Parameter.class);

            if (parameter.name.equals("function") || parameter.name.equals("end")) {
                // only letters allowed!
                parameter.name = "param_" + new Random().nextInt(999);
            }
            return parameter;
        }
    }
}

package moe.knox.factorio.core.parser.api.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphism;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphismClass;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

class TypeJsonAdapter extends TypeAdapter<Type> {
    @Override
    public void write(JsonWriter jsonWriter, Type type) throws IOException {
        // TODO: Implement writing this
        return;
    }

    @Override
    public Type read(JsonReader jsonReader) throws IOException {
        JsonElement jsonElement = new Gson().fromJson(jsonReader, JsonElement.class);
        if (jsonElement.isJsonPrimitive()) {
            Type res = new Type();
            res.type = jsonElement.getAsString();
            return res;
        }
        if (jsonElement.isJsonObject()) {
            Type res = new Type();
            res.isSimpleString = false;

            GsonBuilder builder = new GsonBuilder();
            Helper.addDeserializers(builder);
            res.data = builder
                    .create()
                    .fromJson(jsonElement, Type.ComplexData.class);
            return res;
        }

        return null;
    }
}

/**
 * A type is either a string, in which case that string is the simple type. Otherwise, a type is a table.
 */
@JsonAdapter(TypeJsonAdapter.class)
public class Type {
    public boolean isSimpleString = true;
    public String type;
    public ComplexData data;

    @JsonPolymorphismClass("complexType")
    public class ComplexData {
        @SerializedName("complex_type")
        public String complexType; // A string denoting the kind of complex type.

        @JsonPolymorphism("variant")
        public TypeVariant variant;

        @JsonPolymorphism("array")
        public TypeArray array;

        @JsonPolymorphism({"dictionary", "LuaCustomTable"})
        public TypeDictionary dictionary;

        @JsonPolymorphism("function")
        public TypeFunction function;

        @JsonPolymorphism("LuaLazyLoadedValue")
        public TypeLuaLazyLoadedValue luaLazyLoadedValue;

        @JsonPolymorphism("table")
        public TypeTable table;

        public class TypeVariant {
            public List<Type> options; // A list of all compatible types for aborted this type.
        }

        public class TypeArray {
            public Type value; // The type of the elements of the array.
        }

        public class TypeDictionary {
            public Type key; // The type of the keys of the dictionary or LuaCustomTable.
            public Type value; // The type of the values of the dictionary or LuaCustomTable.
        }

        public class TypeFunction {
            public List<Type> parameters; // The types of the function arguments.
        }

        public class TypeLuaLazyLoadedValue {
            public Type value; // The type of the LuaLazyLoadedValue.
        }

        public class TypeTable {
            public List<Parameter> parameters; // The parameters present in the table.
            public List<ParameterGroup> variant_parameter_groups; // (optional): The optional parameters that depend on one of the main parameters.
            public String variant_parameter_description; // (optional): The text description of the optional parameter groups.
        }

        void sortOrder() {
            if (variant != null && variant.options != null && !variant.options.isEmpty()) {
                variant.options.forEach(type1 -> type1.sortOrder());
            }

            if (array != null && array.value != null) {
                array.value.sortOrder();
            }

            if (dictionary != null) {
                if (dictionary.key != null) {
                    dictionary.key.sortOrder();
                }
                if (dictionary.value != null) {
                    dictionary.value.sortOrder();
                }
            }

            if (function != null && function.parameters != null && !function.parameters.isEmpty()) {
                function.parameters.forEach(type1 -> type1.sortOrder());
            }

            if (luaLazyLoadedValue != null && luaLazyLoadedValue.value != null) {
                luaLazyLoadedValue.value.sortOrder();
            }

            if (table != null) {
                if (table.parameters != null && !table.parameters.isEmpty()) {
                    table.parameters.sort(Comparator.comparingDouble(parameter -> parameter.order));
                    table.parameters.forEach(parameter -> parameter.sortOrder());
                }

                if (table.variant_parameter_groups != null && !table.variant_parameter_groups.isEmpty()) {
                    table.variant_parameter_groups.sort(Comparator.comparingDouble(parameterGroup -> parameterGroup.order));
                    table.variant_parameter_groups.forEach(parameterGroup -> parameterGroup.sortOrder());
                }
            }
        }
    }

    void sortOrder() {
        if (data != null) {
            data.sortOrder();
        }
    }
}

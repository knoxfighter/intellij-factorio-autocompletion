package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.core.parser.api.data.desirealizer.ValueTypeJsonDeserializer;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonAdapter(ValueTypeJsonDeserializer.class)
public interface ValueType extends Arrangeable {
    HashMap<String, Class<? extends ValueType>> TYPES_PER_NATIVE_NAME = new HashMap<>() {{
        put("simple", Simple.class);
        put("array", Array.class);
        put("literal", Literal.class);
        put("type", Type.class);
        put("function", Function.class);
        put("tuple", Tuple.class);
        put("struct", Struct.class);
        put("union", Union.class);
        put("dictionary", Dictionary.class);
        put("LuaCustomTable", LuaCustomTable.class);
        put("table", Table.class);
        put("LuaLazyLoadedValue", LuaLazyLoadedValue.class);
    }};

    private static Map<Class<? extends ValueType>, String> nativeNamesPerType() {
        Map<Class<? extends ValueType>, String> reversedMap = new HashMap<>();

        for (Map.Entry<String, Class<? extends ValueType>> entry : TYPES_PER_NATIVE_NAME.entrySet()) {
            reversedMap.put(entry.getValue(), entry.getKey());
        }

        return reversedMap;
    }

    default String getDescription() {
        return "";
    }

    @Override
    default void arrangeElements() {
    }

    default String getNativeName() {
        return nativeNamesPerType().get(getClass());
    }

    record Simple(String value) implements ValueType {
    }

    record Array(ValueType value) implements ValueType {
    }

    record Literal(String value, String description) implements ValueType {
        public String getDescription() {
            return description;
        }
    }

    record Type(String value, String description) implements ValueType {
        public String getDescription() {
            return description;
        }
    }

    record Function(List<ValueType> parameters) implements ValueType {
    }

    record Tuple(List<TypeTupleParameter> parameters) implements ValueType {
        public record TypeTupleParameter(String name, int order, String description, ValueType type, boolean optional) {
        }
    }

    record Struct(List<StructAttribute> attributes) implements ValueType {
        @Override
        public void arrangeElements() {
            attributes.sort(Comparator.comparingDouble(attribute -> attribute.order));
        }

        public record StructAttribute(
                String name,
                int order,
                String description,
                ValueType type,
                boolean optional,
                boolean read,
                boolean write
        ) {
        }
    }

    record Union(List<ValueType> options, @SerializedName("full_format") boolean fullFormat) implements ValueType {
    }

    record Dictionary(ValueType key, ValueType value) implements ValueType {
    }

    record LuaCustomTable(ValueType key, ValueType value) implements ValueType {
    }

    record Table(
            List<Parameter> parameters,

            @Nullable
            @SerializedName("variant_parameter_groups")
            List<ParameterGroup> variantParameterGroups,

            @SerializedName("variant_parameter_description")
            @Nullable
            String variantParameterDescription
    ) implements ValueType {
        public String getDescription() {
            return variantParameterDescription;
        }

        @Override
        public void arrangeElements() {
            if (variantParameterGroups != null && !variantParameterGroups.isEmpty()) {
                variantParameterGroups.sort(Comparator.comparingDouble(v -> v.order));
                variantParameterGroups.forEach(ParameterGroup::arrangeElements);
            }

            parameters.sort(Comparator.comparingDouble(v -> v.order));
        }
    }

    record LuaLazyLoadedValue(ValueType value) implements ValueType {
    }

    record Unknown() implements ValueType {
    }
}

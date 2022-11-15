package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismType;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismValue;
import moe.knox.factorio.api.parser.deserializer.ValueTypeJsonDeserializer;
import moe.knox.factorio.api.parser.deserializer.ValueTypeLiteralDeserializer;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * A `ValueType` is either a string, in which case that string is the simple type.
 * Otherwise, a type is a table.
 */
@JsonAdapter(ValueTypeJsonDeserializer.class)
@JsonPolymorphismType("complex_type")
public interface ValueType {
    /**
     * String of a simple type.
     */
    record Simple(String value) implements ValueType {}

    /**
     * @param value The actual type. This format for types is used when they have descriptions attached to them.
     * @param description The text description of the type.
     * @since 3
     */
    @JsonPolymorphismValue("type")
    record Type(ValueType value, String description) implements ValueType {}

    /**
     * @param options A list of all compatible types for this type.
     */
    @JsonPolymorphismValue({"variant", "union"})
    record Variant(List<ValueType> options) implements ValueType {}

    /**
     * @param value The type of the elements of the array.
     */
    @JsonPolymorphismValue("array")
    record Array(ValueType value) implements ValueType {}

    /**
     * @param key The type of the keys of the dictionary or LuaCustomTable.
     * @param value The type of the values of the dictionary or LuaCustomTable.
     */
    @JsonPolymorphismValue({"dictionary", "LuaCustomTable"})
    record Dictionary(ValueType key, ValueType value) implements ValueType {}

    /**
     * @param parameters The types of the function arguments.
     */
    @JsonPolymorphismValue("function")
    record Function(List<ValueType> parameters) implements ValueType {}

    /**
     * @param value The value of the literal. Either {@link String}, {@link Double} or {@link Boolean}
     * @param description The text description of the literal, if any.
     * @since 3
     */
    @JsonPolymorphismValue("literal")
    @JsonAdapter(ValueTypeLiteralDeserializer.class)
    record Literal(Object value, @Nullable String description) implements ValueType {}

    /**
     * This Type is a `LuaLazyLoadedValue` class.
     * Which is a generic class with {@link LuaLazyLoadedValue#value} as type.
     * <br>
     * @see <a href="https://lua-api.factorio.com/latest/LuaLazyLoadedValue.html">https://lua-api.factorio.com/latest/LuaLazyLoadedValue.html</a>
     *
     * @param value The type of the LuaLazyLoadedValue.
     */
    @JsonPolymorphismValue("LuaLazyLoadedValue")
    record LuaLazyLoadedValue(ValueType value) implements ValueType {}

    /**
     * @param attributes A list of attributes with the same properties as class attributes.
     * @since 3
     */
    @JsonPolymorphismValue("struct")
    record Struct(List<Attribute> attributes) implements ValueType {}

    @JsonPolymorphismValue("table")
    class Table implements ValueType, PostProcessable {
        /**
         * The parameters present in the table.
         */
        public List<Parameter> parameters;

        /**
         * The optional parameters that depend on one of the main parameters.
         */
        @Nullable
        @SerializedName("variant_parameter_groups")
        public List<ParameterGroup> variantParameterGroups;

        /**
         * The text description of the optional parameter groups.
         */
        @Nullable
        @SerializedName("variant_parameter_description")
        public String variantParameterDescription;

        @Override
        public void postProcess() {
            parameters.sort(Comparator.comparingDouble(value -> value.order));
            if (variantParameterGroups != null) {
                variantParameterGroups.sort(Comparator.comparingDouble(value -> value.order));
            }
        }
    }

    /**
     * Same as {@link ValueType.Table}.
     * It has to be different meaning and implementation, therefore it is its own type.
     */
    @JsonPolymorphismValue("tuple")
    class Tuple implements ValueType, PostProcessable {
        public List<Parameter> parameters;

        @Nullable
        @SerializedName("variant_parameter_groups")
        public List<ParameterGroup> variantParameterGroups;

        @Nullable
        @SerializedName("variant_parameter_description")
        public String variantParameterDescription;

        @Override
        public void postProcess() {
            parameters.sort(Comparator.comparingDouble(value -> value.order));
            if (variantParameterGroups != null) {
                variantParameterGroups.sort(Comparator.comparingDouble(value -> value.order));
            }
        }
    }
}

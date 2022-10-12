package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismType;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismValue;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.OptionalJsonPolymorphismDeserializer;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * The representation of Concepts (they are quite complex)
 * Depending on `category` it has additional members.
 * Since v3, there is no `category` anymore, use `type` instead.
 */
@JsonAdapter(OptionalJsonPolymorphismDeserializer.class)
@JsonPolymorphismType("category")
public class Concept {
    /**
     * The name of the concept.
     */
    public String name;

    /**
     * The order of the concept as shown in the html.
     */
    public double order;

    /**
     * The text description of the concept.
     */
    public String description;

    /**
     * A list of strings containing additional information about the concept.
     */
    @Nullable
    public List<String> notes;

    /**
     * A list of strings containing example code and explanations.
     */
    @Nullable
    public List<String> examples;

    /**
     * A list of strings that are references to other relevant classes or their methods and attributes.
     * @deprecated Removed in v2
     */
    @Deprecated
    @SerializedName("see_also")
    public List<String> seeAlso;

    /**
     * The category of the concept.
     * @deprecated removed in v3. Use {@link Concept#type} instead.
     */
    @Deprecated
    String category;

    /**
     * The type of the concept.
     * This is the replacement for {@link Concept#category}.
     * @since 3
     */
    ValueType type;

    /**
     * A simple collection of parameters.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("table")
    public static class Table extends Concept implements PostProcessable {
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
     * A collection of parameters that optionally drops the explicit keys.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("table_or_array")
    public static class TableOrArray extends Concept implements PostProcessable {
        /**
         * The parameters present in the `table.parameter` groups.
         */
        public List<Parameter> parameters;

        @Override
        public void postProcess() {
            parameters.sort(Comparator.comparingDouble(value -> value.order));
        }
    }

    /**
     * A collection of strings.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("enum")
    public static class Enum extends Concept implements PostProcessable {
        /**
         * The members of the enum.
         */
        public List<BasicMember> options;

        @Override
        public void postProcess() {
            options.sort(Comparator.comparingDouble(value -> value.order));
        }
    }

    /**
     * A collection of flags with special format in the API.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("flag")
    public static class Flag extends Concept implements PostProcessable {
        /**
         * The different flag options.
         */
        public List<BasicMember> options;

        @Override
        public void postProcess() {
            options.sort(Comparator.comparingDouble(value -> value.order));
        }
    }

    /**
     * A list of ways to specify a certain concept within the API.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("union")
    public static class Union extends Concept implements PostProcessable {
        static public class Spec {
            /**
             * The type of the specification option.
             */
            public ValueType type;

            /**
             * The order of the option as shown in the html.
             */
            public double order;

            /**
             * The text description of the option.
             */
            public String description;
        }

        /**
         * A list of specification options.
         */
        public List<Spec> options;

        @Override
        public void postProcess() {
            options.sort(Comparator.comparingDouble(value -> value.order));
        }
    }

    /**
     * An event or prototype filter.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("filter")
    public static class Filter extends Concept implements PostProcessable {
        /**
         * The always-present parameters for the filter.
         */
        public List<Parameter> parameters;

        /**
         * The optional filter parameters that depend on the specific `filter` parameter used.
         */
        @Nullable
        @SerializedName("variant_parameter_groups")
        public List<ParameterGroup> variantParameterGroups;

        /**
         * The text description of the optional filter groups.
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
     * A class-like collection of attributes.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("struct")
    public static class Struct extends Concept implements PostProcessable {
        /**
         * A list of attributes with the same properties as class attributes.
         */
        public List<Attribute> attributes;

        @Override
        public void postProcess() {
            attributes.sort(Comparator.comparingDouble(value -> value.order));
        }
    }

    /**
     * A text-based explanation of a particular format. No additional members.
     * @deprecated removed in v3. Use {@link Concept#type} instead
     */
    @Deprecated
    @JsonPolymorphismValue("concept")
    public static class ConceptValue extends Concept {
    }
}

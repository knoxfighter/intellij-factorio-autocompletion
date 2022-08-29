package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.List;

/**
 * The representation of Concepts (they are quite complex)
 *
 * Depending on `category` it has additional members.
 */
public class Concept implements Arrangeable {
    public String name; // The name of the concept.
    public double order; // The order of the concept as shown in the html.
    public String description; // The text description of the concept.
    public List<String> notes; // (optional): A list of strings containing additional information about the concept.
    public List<String> examples; // (optional): A list of strings containing example code and explanations.
    @SerializedName("see_also")
    public List<String> seeAlso; // (optional): A list of strings that are references to other relevant classes or their methods and attributes.
    public String category; // The category of the concept.
    public CategoryTable table;
    public CategoryTableOrArray tableOrArray;
    public CategoryEnum _enum;
    public CategoryFlag flag;
    public CategoryUnion union;
    public CategoryFilter filter;
    public CategoryStruct struct;

    // If `category` == "table"
    // A simple collection of parameters.
    public class CategoryTable {
        public List<Parameter> parameters; // The parameters present in the table.
        @SerializedName("variant_parameter_groups")
        public List<ParameterGroup> variantParameterGroups; // (optional): The optional parameters that depend on one of the main parameters.
        @SerializedName("variant_parameter_description")
        public String variantParameterDescription; // The text description of the optional parameter groups.
    }

    // If `category` == "table_or_array"
    // A collection of parameters that optionally drops the explicit keys.
    public class CategoryTableOrArray {
        public List<Parameter> parameters; // The parameters present in the table.parameter groups.
    }

    // If `category` == "enum"
    // A collection of strings.
    public class CategoryEnum {
        public List<BasicMember> options; // The members of the enum.
    }

    // If `category` == "flag"
    // A collection of flags with special format in the API.
    public class CategoryFlag {
        public List<BasicMember> options; // The different flag options.
    }

    // If `category` == "union"
    // A list of ways to specify a certain concept within the API.
    public class CategoryUnion {
        public class Spec {
            public Type type; // The type of the specification option.
            public double order; // The order of the option as shown in the html.
            public String description; // The text description of the option.
        }

        public List<Spec> options; // A list of specification options.
    }

    // If `category` == "filter"
    // An event or prototype filter.
    public class CategoryFilter {
        public List<Parameter> parameters; // The always-present parameters for the filter.
        @SerializedName("variant_parameter_groups")
        public List<ParameterGroup> variantParameterGroups; // (optional): The optional filter parameters that depend on the specific filter parameter used.
        @SerializedName("variant_parameter_description")
        public String variantParameterDescription; // (optional): The text description of the optional filter groups.
    }

    // If `category` == "struct"
    // A class-like collection of attributes.
    public class CategoryStruct {
        public List<Attribute> attributes; // A list of attributes with the same properties as class attributes.
    }

    public void arrangeElements() {
        if (table != null) {
            if (table.parameters != null && !table.parameters.isEmpty()) {
                table.parameters.sort(Comparator.comparingDouble(parameter -> parameter.order));
                table.parameters.forEach(Parameter::arrangeElements);
            }
            if (table.variantParameterGroups != null && !table.variantParameterGroups.isEmpty()) {
                table.variantParameterGroups.sort(Comparator.comparingDouble(parameterGroup -> parameterGroup.order));
                table.variantParameterGroups.forEach(ParameterGroup::arrangeElements);
            }
        }

        if (tableOrArray != null && tableOrArray.parameters != null && !tableOrArray.parameters.isEmpty()) {
            tableOrArray.parameters.sort(Comparator.comparingDouble(parameter -> parameter.order));
            tableOrArray.parameters.forEach(Parameter::arrangeElements);
        }

        if (_enum != null && _enum.options != null && !_enum.options.isEmpty()) {
            _enum.options.sort(Comparator.comparingDouble(basicMember -> basicMember.order));
        }

        if (flag != null && flag.options != null && flag.options.isEmpty()) {
            flag.options.sort(Comparator.comparingDouble(basicMember -> basicMember.order));
        }

        if (filter != null) {
            if (filter.parameters != null && !filter.parameters.isEmpty()) {
                filter.parameters.sort(Comparator.comparingDouble(parameter -> parameter.order));
                filter.parameters.forEach(Parameter::arrangeElements);
            }
            if (filter.variantParameterGroups != null && !filter.variantParameterGroups.isEmpty()) {
                this.filter.variantParameterGroups.sort(Comparator.comparingDouble(parameterGroup -> parameterGroup.order));
                this.filter.variantParameterGroups.forEach(ParameterGroup::arrangeElements);
            }
        }

        if (struct != null && struct.attributes != null && !struct.attributes.isEmpty()) {
            struct.attributes.sort(Comparator.comparingDouble(attribute -> attribute.order));
            struct.attributes.forEach(Attribute::arrangeElements);
        }

        if (union!= null && union.options != null && !union.options.isEmpty()) {
            union.options.sort(Comparator.comparingDouble(spec -> spec.order));
        }
    }
}

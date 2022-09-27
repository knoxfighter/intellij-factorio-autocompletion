package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.List;

public class Method implements Arrangable {
    public String name; // The name of the method.
    public double order; // The order of the method as shown in the html.
    public String description; // The text description of the method.
    public List<String> notes; // (optional): A list of strings containing additional information about the method.
    public List<String> examples; // (optional): A list of strings containing example code and explanations.
    @SerializedName("see_also")
    public List<String> seeAlso; // (optional): A list of strings that are references to other relevant classes or their methods and attributes.
    public List<String> subclasses; // (optional): A list of strings specifying the sub-type (of the class) that the method applies to.
    public List<Parameter> parameters; // The parameters of the method. How to interpret them depends on the takes_table member.
    @SerializedName("variant_parameter_groups")
    public List<ParameterGroup> variantParameterGroups; // (optional): The optional parameters that depend on one of the main parameters. Only applies if takes_table is true.
    @SerializedName("variant_parameter_description")
    public String variantParameterDescription; // (optional): The text description of the optional parameter groups.
    @SerializedName("variadic_type")
    public Type variadicType; // (optional): The type of the variadic arguments of the method, if it accepts any.
    @SerializedName("variadic_description")
    public String variadicDescription; // (optional): The description of the variadic arguments of the method, if it accepts any.
    @SerializedName("takes_table")
    public boolean takesTable; // Whether the method takes a single table with named parameters or a sequence of unnamed parameters.
    @SerializedName("table_is_optional")
    public boolean tableIsOptional; // (optional): If takes_table is true, whether that whole table is optional or not.
    @SerializedName("return_type")
    public Type returnType; // (optional): The return type of the method, if it has one.
    @SerializedName("return_description")
    public String returnDescription; // (optional): The description of the return value of the method, if it returns anything.

    public void arrangeElements() {
        if (parameters != null && !parameters.isEmpty()) {
            parameters.sort(Comparator.comparingDouble(parameter -> parameter.order));
            parameters.forEach(Parameter::arrangeElements);
        }

        if (variantParameterGroups != null && !variantParameterGroups.isEmpty()) {
            variantParameterGroups.sort(Comparator.comparingDouble(parameterGroup -> parameterGroup.order));
            variantParameterGroups.forEach(ParameterGroup::arrangeElements);
        }

        if (variadicType != null) {
            variadicType.arrangeElements();
        }

        if (returnType != null) {
            returnType.arrangeElements();
        }
    }
}

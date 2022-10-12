package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class Method implements PostProcessable {
    /**
     * The name of the method.
     */
    public String name;

    /**
     * The order of the method as shown in the html.
     */
    public double order;

    /**
     * The text description of the method.
     */
    public String description;

    /**
     * A list of strings containing additional information about the method.
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
    @Nullable
    @SerializedName("see_also")
    public List<String> seeAlso;

    /**
     * A list of events that this method might raise when called.
     * @since 2
     */
    @Nullable
    public List<EventRaised> raises;

    /**
     * A list of strings specifying the sub-type (of the class) that the method applies to.
     */
    @Nullable
    public List<String> subclasses;

    /**
     * The parameters of the method. How to interpret them depends on the `takes_table` member.
     */
    public List<Parameter> parameters;

    /**
     * The optional parameters that depend on one of the main parameters. Only applies if `takes_table` is `true`.
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

    /**
     * The type of the variadic arguments of the method, if it accepts any.
     */
    @Nullable
    @SerializedName("variadic_type")
    public ValueType variadicType;

    /**
     * The description of the variadic arguments of the method, if it accepts any.
     */
    @Nullable
    @SerializedName("variadic_description")
    public String variadicDescription;

    /**
     * Whether the method takes a single table with named parameters or a sequence of unnamed parameters.
     */
    @SerializedName("takes_table")
    public boolean takesTable;

    /**
     * If `takes_table` is `true`, whether that whole table is optional or not.
     */
    @Nullable
    @SerializedName("table_is_optional")
    public boolean tableIsOptional;

    /**
     * The return type of the method, if it has one.
     * @deprecated Removed in v2
     */
    @Deprecated
    @Nullable
    @SerializedName("return_type")
    public ValueType returnType;

    /**
     * The description of the return value of the method, if it returns anything.
     * @deprecated Removed in v2
     */
    @Deprecated
    @Nullable
    @SerializedName("return_description")
    public String returnDescription;

    /**
     * The return values of this method, which can contain zero, one, or multiple values.
     * Note that these have the same structure as parameters, but do not specify a name.
     * @since 2
     */
    @SerializedName("return_values")
    public List<Parameter> returnValues;

    @Override
    public void postProcess() {
        parameters.sort(Comparator.comparingDouble(value -> value.order));

        if (variantParameterGroups != null) {
            variantParameterGroups.sort(Comparator.comparingDouble(value -> value.order));
        }
    }
}

package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class FactorioClass implements PostProcessable {
    /**
     * The name of the class.
     */
    public String name;

    /**
     * The order of the class as shown in the html.
     */
    public double order;

    /**
     * The text description of the class.
     */
    public String description;

    /**
     * A list of strings containing additional information about the class.
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
     * @deprecated Removed with v2
     */
    @Deprecated
    @SerializedName("see_also")
    public List<String> seeAlso;

    /**
     * The methods that are part of the class.
     */
    public List<Method> methods;

    /**
     * The attributes that are part of the class.
     */
    public List<Attribute> attributes;

    /**
     * Whether the class is never itself instantiated, only inherited from.
     * @since 3
     */
    @SerializedName("abstract")
    public boolean _abstract;

    /**
     * A list of operators on the class. They are called `call`, `index`, or `length` and have the format of either a `Method` or an `Attribute`.
     */
    public List<Operator> operators;

    /**
     * A list of the names of the classes that his class inherits from.
     */
    @Nullable
    @SerializedName("base_classes")
    public List<String> baseClasses;

    @Override
    public void postProcess() {
        methods.sort(Comparator.comparingDouble(value -> value.order));
        methods.sort(Comparator.comparingDouble(value -> value.order));
    }
}

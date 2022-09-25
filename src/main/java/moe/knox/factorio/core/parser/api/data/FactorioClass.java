package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.List;

/**
 * Represents a single class in factorio
 */
public class FactorioClass {
    public String name;
    public double order; // The order of the class as shown in the html.
    public String description;
    public List<String> notes; // (optional): A list of strings containing additional information about the class.
    public List<String> examples; // (optional): A list of strings containing example code and explanations.
    @SerializedName("see_also")
    public List<String> seeAlso; // (optional): A list of strings that are references to other relevant classes or their methods and attributes.
    public List<Method> methods; // The methods that are part of the class.
    public List<Attribute> attributes; // The attributes that are part of the class.
    public List<Operator> operators; // A list of operators on the class. They are called call, index, or length and have the format of either a Method or an Attribute.
    @SerializedName("base_classes")
    public List<String> baseClasses; // (optional): A list of the names of the classes that his class inherits from.

    public void sortOrder() {
        if (methods != null && !methods.isEmpty()) {
            methods.sort(Comparator.comparingDouble(method -> method.order));
            methods.forEach(Method::sortOrder);
        }

        if (attributes != null && !attributes.isEmpty()) {
            attributes.sort(Comparator.comparingDouble(attribute -> attribute.order));
            attributes.forEach(Attribute::sortOrder);
        }
    }
}

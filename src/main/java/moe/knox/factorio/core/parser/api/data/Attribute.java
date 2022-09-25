package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Attribute {
    public String name; // The name of the attribute.
    public double order; // The order of the attribute as shown in the html.
    public String description; // The text description of the attribute.
    public List<String> notes; // (optional): A list of strings containing additional information about the attribute.
    public List<String> examples; // (optional): A list of strings containing example code and explanations.
    @SerializedName("see_also")
    public List<String> seeAlso; // (optional): A list of strings that are references to other relevant classes or their methods and attributes.
    public List<String> subclasses; // (optional): A list of strings specifying the sub-type (of the class) that the attribute applies to.
    public Type type; // The type of the attribute.
    public boolean read; // Whether the attribute can be read from.
    public boolean write; // Whether the attribute can be written to.

    void sortOrder() {
        if (type != null) {
            type.sortOrder();
        }
    }
}

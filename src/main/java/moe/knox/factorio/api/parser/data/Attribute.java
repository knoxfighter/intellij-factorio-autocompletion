package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Attribute {
    /**
     * The name of the attribute.
     */
    public String name;

    /**
     * The order of the attribute as shown in the html.
     */
    public double order;

    /**
     * The text description of the attribute.
     */
    public String description;

    /**
     * A list of strings containing additional information about the attribute.
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
     * A list of events that this attribute might raise when written to.
     * @since 2
     */
    @Nullable
    public List<EventRaised> raises;

    /**
     * A list of strings specifying the sub-type (of the class) that the attribute applies to.
     */
    @Nullable
    public List<String> subclasses;

    /**
     * The type of the attribute.
     */
    public ValueType type;

    /**
     * Whether the attribute is optional or not.
     */
    public boolean optional;

    /**
     * Whether the attribute can be read from.
     */
    public boolean read;

    /**
     * Whether the attribute can be written to.
     */
    public boolean write;
}

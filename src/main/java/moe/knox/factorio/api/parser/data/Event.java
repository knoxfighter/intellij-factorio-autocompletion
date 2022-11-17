package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class Event implements PostProcessable {
    /**
     * The name of the event.
     */
    String name;

    /**
     * The order of the event as shown in the html.
     */
    double order;

    /**
     * The text description of the event.
     */
    String description;

    /**
     * A list of strings containing additional information about the event.
     */
    @Nullable
    List<String> notes;

    /**
     * A list of strings containing example code and explanations.
     */
    @Nullable
    List<String> examples;

    /**
     * A list of strings that are references to other relevant classes or their methods and attributes.
     * @deprecated Removed in v2
     */
    @Deprecated
    @SerializedName("see_also")
    List<String> seeAlso;

    /**
     * The event-specific information that is provided.
     */
    List<Parameter> data;

    @Override
    public void postProcess() {
        data.sort(Comparator.comparingDouble(value -> value.order));
    }
}

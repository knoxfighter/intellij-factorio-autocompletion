package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.List;

/**
 * Represents an event
 */
public class Event implements Arrangeable {
    String name; // The name of the event.
    double order; // The order of the event as shown in the html.
    String description; // The text description of the event.
    List<String> notes; // (optional): A list of strings containing additional information about the event.
    List<String> examples; // (optional): A list of strings containing example code and explanations.
    @SerializedName("see_also")
    List<String> seeAlso; // (optional): A list of strings that are references to other relevant classes or their methods and attributes.
    List<Parameter> data; // The event-specific information that is provided.

    public void arrangeElements() {
        if (data != null && !data.isEmpty()) {
            data.sort(Comparator.comparingDouble(parameter -> parameter.order));
            data.forEach(Parameter::arrangeElements);
        }
    }
}

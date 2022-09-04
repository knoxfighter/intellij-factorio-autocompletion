package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The representation of Concepts (they are quite complex)
 * Depending on `category` it has additional members.
 */
public record Concept(
        String name,
        double order,
        String description,
        @Nullable
        List<String> notes,
        @Nullable
        List<String> examples,
        @Nullable
        @SerializedName("see_also")
        List<String> seeAlso,
        ValueType type
) implements Arrangeable {
    public void arrangeElements() {
        type.arrangeElements();
    }
}

package moe.knox.factorio.api.parser.data;

import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public class Define implements PostProcessable {
    /**
     * The name of the define.
     */
    public String name;

    /**
     * The order of the define as shown in the html.
     */
    public double order;

    /**
     * The text description of the define.
     */
    public String description;

    /**
     * The members of the define.
     */
    @Nullable
    public List<BasicMember> values;

    /**
     * A list of sub-defines.
     */
    @Nullable
    public List<Define> subkeys;

    @Override
    public void postProcess() {
        if (values != null) {
            values.sort(Comparator.comparingDouble(value -> value.order));
        }
        if (subkeys != null) {
            subkeys.sort(Comparator.comparingDouble(value -> value.order));
        }
    }
}

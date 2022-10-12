package moe.knox.factorio.api.parser.data;

import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;
import org.jetbrains.annotations.Nullable;

public class Parameter implements PostProcessable {
    /**
     * The name of the parameter.
     */
    @Nullable
    public String name;

    /**
     * The order of the parameter as shown in the html.
     */
    public double order;

    /**
     * The text description of the parameter.
     */
    public String description;

    /**
     * The type of the parameter.
     */
    public ValueType type;

    /**
     * Whether the type is optional or not.
     */
    public boolean optional;

    private static int currentParam = 0;

    @Override
    public void postProcess() {
        if (name != null && (name.equals("function") || name.equals("end"))) {
            // only letters allowed!
            name = "param_" + currentParam;
            ++currentParam;
        }
    }
}

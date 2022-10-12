package moe.knox.factorio.api.parser.data;

import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;

import java.util.Comparator;
import java.util.List;

public class ParameterGroup implements PostProcessable {
    /**
     * The name of the parameter group.
     */
    public String name;

    /**
     * The order of the parameter group as shown in the html.
     */
    public double order;
    /**
     * The text description of the parameter group.
     */
    public String description;

    /**
     * The parameters that the group adds.
     */
    public List<Parameter> parameters;

    @Override
    public void postProcess() {
        parameters.sort(Comparator.comparingDouble(value -> value.order));
    }
}

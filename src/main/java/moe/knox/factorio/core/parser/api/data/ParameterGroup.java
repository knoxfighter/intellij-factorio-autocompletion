package moe.knox.factorio.core.parser.api.data;

import java.util.Comparator;
import java.util.List;

public class ParameterGroup implements Arrangeable {
    public String name; // The name of the parameter group.
    public double order; // The order of the parameter group as shown in the html.
    public String description; // The text description of the parameter group.
    public List<Parameter> parameters; // The parameters that the group adds.

    public void arrangeElements() {
        if (parameters != null && !parameters.isEmpty()) {
            parameters.sort(Comparator.comparingDouble(parameter -> parameter.order));
            parameters.forEach(Parameter::arrangeElements);
        }
    }
}

package moe.knox.factorio.core.parser.api.data;

import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphism;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphismClass;


@JsonPolymorphismClass("name")
public class Operator implements Arrangeable {
    /**
     * "index", "length": Attributes
     * "call": Method
     */
    public String name;

    @JsonPolymorphism("call")
    public Method method;

    @JsonPolymorphism({"index", "length"})
    public Attribute attribute;

    public void arrangeElements() {
        if (method != null) {
            method.arrangeElements();
        }

        if (attribute != null) {
            attribute.arrangeElements();
        }
    }
}

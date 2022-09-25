package moe.knox.factorio.core.parser.api.data;

import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphism;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphismClass;


@JsonPolymorphismClass("name")
public class Operator {
    /**
     * "index", "length": Attributes
     * "call": Method
     */
    public String name;

    @JsonPolymorphism("call")
    public Method method;

    @JsonPolymorphism({"index", "length"})
    public Attribute attribute;

    void sortOrder() {
        if (method != null) {
            method.sortOrder();
        }

        if (attribute != null) {
            attribute.sortOrder();
        }
    }
}

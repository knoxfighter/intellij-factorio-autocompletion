package moe.knox.factorio.core.parser.apiData;

import moe.knox.factorio.core.parser.apiData.JsonPolymorphism.JsonPolymorphism;
import moe.knox.factorio.core.parser.apiData.JsonPolymorphism.JsonPolymorphismClass;


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

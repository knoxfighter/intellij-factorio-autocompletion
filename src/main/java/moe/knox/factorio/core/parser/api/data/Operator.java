package moe.knox.factorio.core.parser.api.data;

import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphism;
import moe.knox.factorio.core.parser.api.data.JsonPolymorphism.JsonPolymorphismClass;

@JsonPolymorphismClass("name")
public class Operator implements Arrangeable {
    final public String name;
    final public @JsonPolymorphism("call") Method method;
    final public @JsonPolymorphism({"index", "length"}) Attribute attribute;

    public Operator(String name, Method method, Attribute attribute) {
        this.name = name;
        this.method = method;
        this.attribute = attribute;
    }

    public void arrangeElements() {
        if (method != null) {
            method.arrangeElements();
        }

        if (attribute != null) {
            attribute.arrangeElements();
        }
    }

    public Type getType()
    {
        return Type.fromNativeName(name);
    }

    public enum Type {
        LENGTH("length"),
        INDEX("index"),
        CALL("call");

        private final String nativeName;

        Type(String nativeName) {
            this.nativeName = nativeName;
        }

        public static Type fromNativeName(String nativeName) {
            for (Type b : Type.values()) {
                if (b.nativeName.equals(nativeName)) {
                    return b;
                }
            }

            return null;
        }
    }
}

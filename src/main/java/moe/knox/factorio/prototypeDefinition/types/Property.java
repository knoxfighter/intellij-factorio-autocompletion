package moe.knox.factorio.prototypeDefinition.types;

import com.google.gson.annotations.SerializedName;

public class Property {
    public String name;
    public String type;
    String description;
    @SerializedName("default")
    public String _default;
    boolean optional;
}

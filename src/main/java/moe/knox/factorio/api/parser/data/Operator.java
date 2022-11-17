package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.JsonAdapter;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismDeserializer;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismType;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismValue;

@JsonAdapter(JsonPolymorphismDeserializer.class)
@JsonPolymorphismType("name")
public interface Operator {
    @JsonPolymorphismValue("call")
    class Method extends moe.knox.factorio.api.parser.data.Method implements Operator {}

    @JsonPolymorphismValue({"index", "length"})
    class Attribute extends moe.knox.factorio.api.parser.data.Attribute implements Operator {}
}

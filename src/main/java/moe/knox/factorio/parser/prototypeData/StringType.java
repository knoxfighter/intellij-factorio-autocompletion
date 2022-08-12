package moe.knox.factorio.parser.prototypeData;

import java.util.Map;

public class StringType extends PrototypeParent {
	public Map<String, String> value;

	public StringType() {}

	public StringType(Map<String, String> value) {
		this.value = value;
	}

	public StringType(PrototypeParent parent, Map<String, String> value) {
		super(parent);
		this.value = value;
	}

	public StringType(StringType type) {
		super(type);
		this.value = type.value;
	}
}

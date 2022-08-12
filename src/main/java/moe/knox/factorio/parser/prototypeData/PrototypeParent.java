package moe.knox.factorio.parser.prototypeData;

public class PrototypeParent {
	public String name;
	public String link;
	public String description;

	// default constructor
	public PrototypeParent() {}

	// copy constructor
	public PrototypeParent(PrototypeParent parent) {
		this.name = parent.name;
		this.link = parent.link;
		this.description = parent.description;
	}

}

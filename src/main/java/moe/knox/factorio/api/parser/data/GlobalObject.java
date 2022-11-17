package moe.knox.factorio.api.parser.data;

/**
 * Definition of global objects (just variables in global scope)
 */
public class GlobalObject {
    /**
     * The global variable name of the object.
     */
    public String name;

    /**
     * The order of the global object as shown in the html.
     */
    public double order;

    /**
     * The text description of the global object.
     */
    public String description;

    /**
     * The class name of the global object.
     */
    public String type;
}

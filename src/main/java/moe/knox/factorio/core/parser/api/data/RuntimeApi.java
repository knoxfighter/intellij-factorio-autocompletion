package moe.knox.factorio.core.parser.api.data;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.List;

/**
 * The representation of the new json lua-api
 */
public class RuntimeApi implements Arrangable {
    /**
     * The application this documentation is for.
     * Will always be "factorio".
     */
    public String application;

    /**
     * Indicates the stage this documentation is for.
     * Will always be "runtime" (as opposed to "data"; see the data lifecycle for more detail).
     */
    public String stage;

    /**
     * The version of the game that this documentation is for.
     * An example would be "1.1.30".
     */
    public String application_version;

    /**
     * The version of the machine-readable format itself.
     * It is incremented every time the format changes.
     * The version this documentation reflects is stated at the top.
     * <p>
     * Currently: 1
     */
    public String api_version;

    /**
     * The list of classes (LuaObjects) the API provides.
     * Equivalent to the `classes` page.
     */
    public List<FactorioClass> classes;

    /**
     * The list of events that the API provides.
     * Equivalent to the `events` page.
     */
    public List<Event> events;

    /**
     * The list of defines that the game uses.
     * Equivalent to the `defines` page.
     */
    public List<Define> defines;

    /**
     * The list of types that are built into Lua itself.
     * Equivalent to the `built-in` types page.
     */
    @SerializedName("builtin_types")
    public List<BuiltinType> builtinTypes;

    /**
     * The list of concepts of various types that the API uses.
     * Equivalent to the `concepts` page.
     */
    public List<Concept> concepts;

    /**
     * The list of objects that the game provides as global variables to serve as entry points to the API.
     */
    @SerializedName("global_objects")
    public List<GlobalObject> globalObjects;

    public void arrangeElements() {
        if (classes != null && !classes.isEmpty()) {
            classes.sort(Comparator.comparingDouble(factorioClass -> factorioClass.order));
            classes.forEach(FactorioClass::arrangeElements);
        }

        if (events != null && !events.isEmpty()) {
            events.sort(Comparator.comparingDouble(event -> event.order));
            events.forEach(Event::arrangeElements);
        }

        if (defines != null && !defines.isEmpty()) {
            defines.sort(Comparator.comparingDouble(define -> define.order));
            defines.forEach(Define::arrangeElements);
        }

        if (builtinTypes != null && !builtinTypes.isEmpty()) {
            builtinTypes.sort(Comparator.comparingDouble(builtinType -> builtinType.order));
        }

        if (concepts != null && !concepts.isEmpty()) {
            concepts.sort(Comparator.comparingDouble(concept -> concept.order));
            concepts.forEach(Concept::arrangeElements);
        }

        if (globalObjects != null && !globalObjects.isEmpty()) {
            globalObjects.sort(Comparator.comparingDouble(globalObject -> globalObject.order));
        }
    }
}

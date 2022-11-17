package moe.knox.factorio.api.parser.data;

import com.google.gson.annotations.SerializedName;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessable;

import java.util.Comparator;
import java.util.List;

public class RuntimeApi implements PostProcessable {
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
    @SerializedName("application_version")
    public String applicationVersion;

    /**
     * The version of the machine-readable format itself.
     * It is incremented every time the format changes.
     * The version this documentation reflects is stated at the top.
     * <p>
     * Currently: 2
     */
    @SerializedName("api_version")
    public int apiVersion;

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

    /**
     * The list of functions that the game provides as global variables to provide some specific functionality.
     * @since 3
     */
    @SerializedName("global_functions")
    public List<Method> globalFunctions;

    @Override
    public void postProcess() {
        classes.sort(Comparator.comparingDouble(value -> value.order));
        events.sort(Comparator.comparingDouble(value -> value.order));
        defines.sort(Comparator.comparingDouble(value -> value.order));
        builtinTypes.sort(Comparator.comparingDouble(value -> value.order));
        concepts.sort(Comparator.comparingDouble(value -> value.order));
        globalObjects.sort(Comparator.comparingDouble(value -> value.order));
    }
}

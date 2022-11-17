package moe.knox.factorio.api.parser.data;

/**
 * @since 2
 */
public class EventRaised {
    /**
     * The name of the event being raised.
     */
    public String name;

    /**
     * The order of the member as shown in the html.
     */
    public double order;

    /**
     * The text description of the raised event.
     */
    public String description;

    /**
     * The timeframe during which the event is raised. One of "instantly", "current_tick", or "future_tick".
     */
    public String timeframe;

    /**
     * Whether the event is always raised, or only dependant on a certain condition.
     */
    public boolean optional;
}

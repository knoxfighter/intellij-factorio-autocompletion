package moe.knox.factorio.core.parser.api.data;

import java.util.Comparator;
import java.util.List;

/**
 * The representation of a define (defines.?.?)
 */
public class Define implements Arrangeable {
    /**
     * The name of the define.
     */
    public String name;

    /**
     * The order of the define as shown in the html.
     */
    public double order;

    /**
     * The text description of the define.
     */
    public String description;

    /**
     * The members of the define.
     * -Optional-
     */
    public List<BasicMember> values;

    /**
     * A list of sub-defines.
     * -Optional-
     */
    public List<Define> subkeys;

    public void arrangeElements() {
        if (values != null && !values.isEmpty()) {
            values.sort(Comparator.comparingDouble(basicMember -> basicMember.order));
        }

        if (subkeys != null && !subkeys.isEmpty()) {
            subkeys.sort(Comparator.comparingDouble(define -> define.order));
            subkeys.forEach(Define::arrangeElements);
        }
    }
}

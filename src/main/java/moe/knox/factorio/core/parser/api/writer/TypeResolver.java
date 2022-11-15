package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.Parameter;
import moe.knox.factorio.api.parser.data.ValueType;

import java.util.ArrayList;
import java.util.List;

// TODO: use parameter groups again (can use overloads for now)

final class TypeResolver
{
    private static int UID = 0;
    static class AdditionalType {
        String name;
        List<Parameter> parameters;

        public AdditionalType(List<Parameter> parameters) {
            // TODO: use human understandable names
            this.name = "added_type_" + UID;
            UID++;
            this.parameters = parameters;
        }
    }
    static List<AdditionalType> additionalTypes = new ArrayList<>();

    /**
     * add parameters to the additional types to be created later.
     * @return unique name of the new additional type.
     */
    static String presentTableParams(List<Parameter> parameters) {
        AdditionalType additionalType = new AdditionalType(parameters);
        additionalTypes.add(additionalType);
        return additionalType.name;
    }

    static String getType(ValueType type) {
        String result;
        if (type instanceof ValueType.Simple simple) {
            return simple.value();
        } else if (type instanceof ValueType.Variant union) {
            result = presentUnion(union);
        } else if (type instanceof ValueType.Array array) {
            result = presentArray(array);
        } else if (type instanceof ValueType.Dictionary dictionary) {
            result = presentDictionary(dictionary);
        } else if (type instanceof ValueType.Function function) {
            result = presentFunction(function);
        } else if (type instanceof ValueType.Table table) {
            result = presentTable(table);
        } else if (type instanceof ValueType.Tuple tuple) {
            // TODO: implement tuple (EmmyLUA does not support typed tuples)
//            result = presentTuple(tuple);
            result = "any";
        } else if (type instanceof ValueType.LuaLazyLoadedValue) {
            // TODO override `LuaLazyLoadedValue` class with generic
            // knox: Disabled until further rework is done
//            result = type.getNativeName();
            result = "any";
        } else if (type instanceof ValueType.Type type1) {
            result = presentType(type1);
        } else if (type instanceof ValueType.Literal literal) {
            result = presentLiteral(literal);
        } else {
            throw new IllegalStateException("Unknown type");
        }

        return result;
    }

    /**
     * @return String in format {@code "table<A, B>"}
     */
    private static String presentDictionary(ValueType.Dictionary type) {
        return "table<" + getType(type.key()) + ", " + getType(type.value()) + ">";
    }

    /**
     * @return String in format {@code "fun(param:A, param2:B):RETURN_TYPE"}
     */
    private static String presentFunction(ValueType.Function type) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fun(");
        int i = 0;
        for (ValueType parameter : type.parameters()) {
            if (i > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append("param").append(i).append(':').append(getType(parameter));
            ++i;
        }
        stringBuilder.append(")");

        return stringBuilder.toString();
    }

    /**
     * @return String in format {@code "TYPE1|TYPE2"}
     */
    private static String presentUnion(ValueType.Variant type) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (ValueType option : type.options()) {
            String actualType = getType(option);
            if (!first) {
                stringBuilder.append('|');
            }
            first = false;
            stringBuilder.append(actualType);
        }

        return stringBuilder.toString();
    }

    /**
     * @return String in format {@code "TYPE[]"}
     */
    private static String presentArray(ValueType.Array type) {
        StringBuilder stringBuilder = new StringBuilder();
        // A[]
        try {
            stringBuilder.append(getType(type.value())).append("[]");
        } catch (NullPointerException e) {
            e.printStackTrace(); // TODO: check it, where does it throw that NPE potentially?
        }

        return stringBuilder.toString();
    }

    /**
     * @see TypeResolver#presentTableParams
     */
    private static String presentTable(ValueType.Table type) {
        return presentTableParams(type.parameters);
        // TODO: add variant parameter groups
    }

    private static String presentType(ValueType.Type typeWithDescription) {
        // Only return the type here
        // The description should only be used for params and therefore can be added as `@param <word> <text>` to the overlying function itself.
        return getType(typeWithDescription.value());
    }

    private static String presentLiteral(ValueType.Literal type) {
        // TODO how present non-string literals ??
        // Only available for string, do nothing for every other type (for now)
        if (type.value() instanceof String) {
            return "\"" + type.value() + "\"";
        }
        // TODO: present literals for all types.
        return "";
    }
}

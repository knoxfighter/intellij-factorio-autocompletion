package moe.knox.factorio.core.parser.api.writer;

import moe.knox.factorio.api.parser.data.Parameter;
import moe.knox.factorio.api.parser.data.ValueType;

import java.util.List;

final class AnnotationTypeResolver
{
    /**
     * @return String in format {@code "{["huhu"]:number, ["baum"]:string}"}
     */
    static String presentTableParams(List<Parameter> parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        boolean first = true;
        for (Parameter parameter : parameters) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            stringBuilder.append("[\"").append(parameter.name).append("\"]:").append(getType(parameter.type));
            if (parameter.optional) {
                stringBuilder.append("|nil");
            }
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    static String getType(ValueType type) {
        String result;
        if (type instanceof ValueType.Simple simple) {
            return simple.value();
        } else if (type instanceof ValueType.Union union) {
            result = presentUnion(union);
        } else if (type instanceof ValueType.Array array) {
            result = presentArray(array);
        } else if (type instanceof ValueType.LuaCustomTable luaCustomTable) {
            result = luaCustomTableType(luaCustomTable);
        } else if (type instanceof ValueType.Dictionary dictionary) {
            result = presentDictionary(dictionary);
        } else if (type instanceof ValueType.Function function) {
            result = presentFunction(function);
        } else if (type instanceof ValueType.Table table) {
            result = presentTable(table);
        } else if (type instanceof ValueType.LuaLazyLoadedValue) {
            // TODO override `LuaLazyLoadedValue` class with generic
            result = type.getNativeName();
        } else if (type instanceof ValueType.Tuple tuple) {
            result = presentTuple(tuple);
        } else if (type instanceof ValueType.Type type1) {
            result = presentType(type1);
        } else if (type instanceof ValueType.Literal literal) {
            result = presentLiteral(literal);
        } else {
            throw new IllegalStateException("Unexpected value: " + type.getNativeName());
        }

        return result;
    }

    /**
     * @return String in format {@code "table<A, B>"}
     */
    private static String luaCustomTableType(ValueType.LuaCustomTable type) {
        return "table<" + getType(type.key()) + ", " + getType(type.value()) + ">";
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
    private static String presentUnion(ValueType.Union type) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (ValueType option : type.options()) {
            if (!first) {
                stringBuilder.append('|');
            }
            first = false;
            stringBuilder.append(getType(option));
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
            e.printStackTrace(); // todo check it
        }

        return stringBuilder.toString();
    }

    /**
     * @return String in format {@code "{["huhu"]:number, ["baum"]:string}"}
     */
    private static String presentTable(ValueType.Table type) {
        return presentTableParams(type.parameters());
    }

    private static String presentTuple(ValueType.Tuple type) {
        // TODO how present tuple ??
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (ValueType.Tuple.TypeTupleParameter parameter : type.parameters()) {
            if (!first) {
                stringBuilder.append(',');
            }
            first = false;
            stringBuilder
                    .append(getType(parameter.type()))
                    .append(" ")
                    .append(parameter.name())
            ;
        }

        return stringBuilder.toString();
    }

    private static String presentType(ValueType.Type typeWithDescription) {
        // TODO how present type ??
        return typeWithDescription.value();
    }

    private static String presentLiteral(ValueType.Literal type) {
        // TODO how present literal ??
        return "\"" + type.value() + "\"";
    }
}

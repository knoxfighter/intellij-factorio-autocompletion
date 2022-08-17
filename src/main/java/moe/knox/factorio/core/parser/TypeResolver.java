package moe.knox.factorio.core.parser;

import moe.knox.factorio.core.parser.apiData.Parameter;
import moe.knox.factorio.core.parser.apiData.Type;

import java.util.List;

final class TypeResolver
{
    static String getType(Type type) {
        if (type.isSimpleString) {
            return type.type;
        }

        Type.ComplexData data = type.data;
        switch (data.complexType) {
            case "variant": {
                StringBuilder stringBuilder = new StringBuilder();
                boolean first = true;
                for (Type option : data.variant.options) {
                    if (!first) {
                        stringBuilder.append('|');
                    }
                    first = false;
                    stringBuilder.append(getType(option));
                }

                return stringBuilder.toString();
            }
            case "array": {
                StringBuilder stringBuilder = new StringBuilder();
                // A[]
                try {
                    stringBuilder.append(getType(data.array.value)).append("[]");
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                return stringBuilder.toString();
            }
            case "dictionary":
            case "LuaCustomTable": {
                StringBuilder stringBuilder = new StringBuilder();
                // table<A, B>
                stringBuilder.append("table<").append(getType(data.dictionary.key)).append(", ").append(getType(data.dictionary.value)).append(">");
                return stringBuilder.toString();
            }
            case "function": {
                StringBuilder stringBuilder = new StringBuilder();
                // fun(param:A, param2:B):RETURN_TYPE
                stringBuilder.append("fun(");
                int i = 0;
                for (Type parameter : data.function.parameters) {
                    if (i > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append("param").append(i).append(':').append(getType(parameter));
                    ++i;
                }
                stringBuilder.append(")");
            }
            case "LuaLazyLoadedValue": {
                return "LuaLazyLoadedValue";
                // TODO override `LuaLazyLoadedValue` class with generic
            }
            case "table": {
                return getAnonymousTableType(data.table.parameters);
            }
            default:
                throw new IllegalStateException("Unexpected value: " + data);
        }
    }

    static String getAnonymousTableType(List<Parameter> parameters) {
        // {["huhu"]:number, ["baum"]:string}
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
}

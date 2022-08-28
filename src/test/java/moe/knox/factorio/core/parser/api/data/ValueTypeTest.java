package moe.knox.factorio.core.parser.api.data;

import junit.framework.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ValueTypeTest extends TestCase {

    @BeforeEach
    protected void setUp() {
    }

    public static Stream<Arguments> providerTestTypesPerNativeName() {
        return Stream.of(
                Arguments.of("simple", new ValueType.Simple("boolean")),
                Arguments.of("array", new ValueType.Array(new ValueType.Simple("int"))),
                Arguments.of("LuaLazyLoadedValue", new ValueType.LuaLazyLoadedValue(new ValueType.Simple("int")))
        );
    }

    @ParameterizedTest
    @MethodSource("providerTestTypesPerNativeName")
    public void testTypesPerNativeName(String expectedNativeName, ValueType valueType) {
        assertEquals(valueType.getNativeName(), expectedNativeName);
    }
}
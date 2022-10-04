package moe.knox.factorio.core.parser.api;

import junit.framework.TestCase;
import moe.knox.factorio.core.version.FactorioVersionResolver;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.core.parser.api.data.RuntimeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;

public class RuntimeApiParserTest extends TestCase {
    private RuntimeApiParser service;

    @BeforeEach
    protected void setUp() {
        service = new RuntimeApiParser();
    }

    public static Set<FactorioVersion> providerVersions() throws IOException {
        return (new FactorioVersionResolver()).supportedVersions();
    }

    @ParameterizedTest
    @MethodSource("providerVersions")
    void parse(FactorioVersion version) {
        RuntimeApi runtimeApi = service.parse(version);

        assertNotNull(runtimeApi);
        assertNotNull(runtimeApi.api_version);
        assertNotNull(runtimeApi.classes);
        assertNotNull(runtimeApi.concepts);
        assertNotNull(runtimeApi.defines);
    }
}
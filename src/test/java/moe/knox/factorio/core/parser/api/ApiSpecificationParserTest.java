package moe.knox.factorio.core.parser.api;

import junit.framework.TestCase;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.core.parser.api.data.RuntimeApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;

public class ApiSpecificationParserTest extends TestCase {
    private ApiSpecificationParser service;

    @BeforeEach
    protected void setUp() {
        service = new ApiSpecificationParser();
    }

    public static Set<FactorioApiVersion> providerVersions() throws IOException {
        return (new ApiVersionResolver()).supportedVersions();
    }

    @ParameterizedTest
    @MethodSource("providerVersions")
    void parse(FactorioApiVersion version) {
        RuntimeApi runtimeApi = service.parse(version);

        assertNotNull(runtimeApi);
    }
}
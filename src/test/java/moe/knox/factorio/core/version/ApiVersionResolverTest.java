package moe.knox.factorio.core.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class ApiVersionResolverTest {

    private ApiVersionResolver apiVersionResolver;

    @BeforeEach
    protected void setUp() {
        apiVersionResolver = new ApiVersionResolver();
    }

    @Test
    void supportedVersions() throws IOException {
        var versions = apiVersionResolver.supportedVersions();

        Assertions.assertFalse(versions.isEmpty(), "Versions cant be empty");
    }
}
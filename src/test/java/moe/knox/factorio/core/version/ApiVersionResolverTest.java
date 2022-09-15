package moe.knox.factorio.core.version;

import junit.framework.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ApiVersionResolverTest extends TestCase {

    private ApiVersionResolver apiVersionResolver;

    @BeforeEach
    protected void setUp() {
        apiVersionResolver = new ApiVersionResolver();
    }

    @Test
    void supportedVersions() throws IOException {
        var versions = apiVersionResolver.supportedVersions();

        assertFalse("Versions cant be empty", versions.isEmpty());
    }
}
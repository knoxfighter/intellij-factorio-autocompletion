package moe.knox.factorio.core.version;

import junit.framework.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class FactorioVersionResolverTest extends TestCase {

    private FactorioVersionResolver factorioVersionResolver;

    @BeforeEach
    protected void setUp() {
        factorioVersionResolver = new FactorioVersionResolver();
    }

    @Test
    void supportedVersions() throws IOException {
        var versions = factorioVersionResolver.supportedVersions();

        assertFalse("Versions cant be empty", versions.isEmpty());
    }
}
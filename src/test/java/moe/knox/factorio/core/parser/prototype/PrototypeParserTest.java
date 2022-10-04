package moe.knox.factorio.core.parser.prototype;

import junit.framework.TestCase;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class PrototypeParserTest extends TestCase {

    private static Path tempDir;
    private PrototypeParser prototypeParser;

    @BeforeAll
    protected static void setUpAll(@TempDir(cleanup = CleanupMode.NEVER) Path tempDirArg)
    {
        tempDir = tempDirArg;
    }

    @BeforeEach
    protected void setUp() {
        Path prototypeParserRootPath = tempDir.resolve("prototypes");

        prototypeParser = new PrototypeParser(prototypeParserRootPath);
    }

    public static Set<FactorioApiVersion> providerVersions() throws IOException {
        return (new ApiVersionResolver()).supportedVersions();
    }

    @ParameterizedTest
    @MethodSource("providerVersions")
    void parse(FactorioApiVersion version) throws IOException {
        prototypeParser.parse(version);
    }
}
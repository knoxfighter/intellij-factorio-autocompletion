package moe.knox.factorio.core.parser.prototype;

import junit.framework.TestCase;
import moe.knox.factorio.core.version.FactorioVersionResolver;
import moe.knox.factorio.core.version.FactorioVersion;
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

    public static Set<FactorioVersion> providerVersions() throws IOException {
        return (new FactorioVersionResolver()).supportedVersions();
    }

    @ParameterizedTest
    @MethodSource("providerVersions")
    void parse(FactorioVersion version) throws IOException {
        prototypeParser.parse(version);
    }
}
package moe.knox.factorio.core;

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

public class LuaLibDownloaderTest extends TestCase {
    private static Path tempDir;

    private LuaLibDownloader luaLibDownloader;

    @BeforeAll
    protected static void setUpAll(@TempDir(cleanup = CleanupMode.NEVER) Path tempDirArg)
    {
        tempDir = tempDirArg;
    }

    @BeforeEach
    protected void setUp() {
        Path luaLibRootPath = tempDir.resolve("lualib");
        Path corePrototypesRootPath = tempDir.resolve("core_prototypes");

        luaLibDownloader = new LuaLibDownloader(luaLibRootPath, corePrototypesRootPath);
    }

    public static Set<FactorioVersion> providerVersions() throws IOException {
        return (new FactorioVersionResolver()).supportedVersions();
    }

    @ParameterizedTest
    @MethodSource("providerVersions")
    void downloadAll(FactorioVersion version) throws GettingTagException, IOException {
        luaLibDownloader.downloadAll(version);
    }
}
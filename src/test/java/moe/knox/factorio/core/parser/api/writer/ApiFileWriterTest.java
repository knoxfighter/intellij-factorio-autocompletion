package moe.knox.factorio.core.parser.api.writer;

import junit.framework.TestCase;
import moe.knox.factorio.core.parser.api.ApiSpecificationParser;
import moe.knox.factorio.core.parser.api.data.RuntimeApi;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiFileWriterTest extends TestCase {
    @Test
    void writeRuntimeApi(@TempDir(cleanup = CleanupMode.NEVER) Path tempDir) throws IOException {
        var parser = new ApiSpecificationParser();
        var version = (new ApiVersionResolver()).supportedVersions().latestVersion();
        var runtimeApi = parser.parse(version);

        var outputFileName = tempDir.toString() + "/factorio.lua";

        try (java.io.Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)))) {
            ApiFileWriter.fromIoWriter(writer).writeRuntimeApi(runtimeApi);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
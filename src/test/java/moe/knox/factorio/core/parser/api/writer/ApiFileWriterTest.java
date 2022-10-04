package moe.knox.factorio.core.parser.api.writer;

import junit.framework.TestCase;
import moe.knox.factorio.core.parser.api.RuntimeApiParser;
import moe.knox.factorio.core.version.ApiVersionResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;

public class ApiFileWriterTest extends TestCase {
    @Test
    void writeRuntimeApi(@TempDir(cleanup = CleanupMode.NEVER) Path tempDir) throws IOException {
        var parser = new RuntimeApiParser();
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
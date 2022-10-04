package moe.knox.factorio.core.parser.api;

import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.core.parser.api.writer.ApiFileWriter;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.core.parser.api.data.*;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ApiParser {
    private final Path apiRootPath;
    private final RuntimeApiParser runtimeApiParser;

    public ApiParser(Path apiRootPath) {
        this.apiRootPath = apiRootPath;
        runtimeApiParser = new RuntimeApiParser();
    }

    public @Nullable Path getApiPath(FactorioVersion version) {
        Path versionPath = getVersionPath(version);

        return Files.exists(versionPath) ? versionPath : null;
    }

    public void removeCurrentAPI() {
        FileUtil.delete(apiRootPath.toFile());
    }

    public void parse(FactorioVersion version) throws IOException {
        Path versionPath = getVersionPath(version);

        Files.createDirectories(versionPath);

        RuntimeApi runtimeApi = runtimeApiParser.parse(version);

        var outputFileName = versionPath.resolve("factorio.lua").toFile();

        try (var writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFileName)))) {
            ApiFileWriter.fromIoWriter(writer).writeRuntimeApi(runtimeApi);

            writer.flush();
        }
    }

    private Path getVersionPath(FactorioVersion version) {
        return apiRootPath.resolve(version.version());
    }
}

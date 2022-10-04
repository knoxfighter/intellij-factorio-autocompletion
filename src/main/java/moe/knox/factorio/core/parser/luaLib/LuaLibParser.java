package moe.knox.factorio.core.parser.luaLib;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.core.version.FactorioVersion;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final public class LuaLibParser {
    private static final Logger LOG = Logger.getInstance(LuaLibParser.class);
    private static final String luaLibGithubTagsZipLink = "https://api.github.com/repos/wube/factorio-data/zipball";

    private final Path luaLibRootPath;
    private final Path corePrototypeRootPath;

    public LuaLibParser(Path luaLibRootPath, Path corePrototypeRootPath) {
        this.luaLibRootPath = luaLibRootPath;
        this.corePrototypeRootPath = corePrototypeRootPath;
    }

    public void removeLuaLibFiles() {
        FileUtil.delete(luaLibRootPath.toFile());
        FileUtil.delete(corePrototypeRootPath.toFile());
    }

    public @Nullable Path getLuaLibPath(FactorioVersion version) {
        Path versionPath = luaLibRootPath.resolve(version.version());

        return Files.exists(versionPath) ? versionPath : null;
    }

    public @Nullable Path getPrototypePath(FactorioVersion version) {
        Path versionPath = corePrototypeRootPath.resolve(version.version());

        return Files.exists(versionPath) ? versionPath : null;
    }

    public void downloadAll(FactorioVersion version) throws IOException {
        Path luaLibRootPathSubDir = luaLibRootPath.resolve(version.version());
        Path corePrototypeSubDir = corePrototypeRootPath.resolve(version.version()).resolve("core");
        Path basePrototypeSubDir = corePrototypeRootPath.resolve(version.version()).resolve("base");

        Files.createDirectories(luaLibRootPathSubDir);
        Files.createDirectories(corePrototypeSubDir);
        Files.createDirectories(basePrototypeSubDir);

        URL url = new URL(luaLibGithubTagsZipLink + "/" + version.version());

        InputStream inputStream = url.openStream();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        // Iterate over all files in the zip and only save the needed
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            saveZipEntry(zipInputStream, zipEntry, "/lualib/", luaLibRootPathSubDir);
            saveZipEntry(zipInputStream, zipEntry, "/core/prototypes/", corePrototypeSubDir);
            saveZipEntry(zipInputStream, zipEntry, "/base/prototypes/", basePrototypeSubDir);
        }
    }

    private void saveZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, String inZipDir, Path toSaveDir) throws IOException {
        int pos = zipEntry.getName().lastIndexOf(inZipDir);

        if (pos == -1) {
            return;
        }

        // This thing is inside core-prototype
        String filename = zipEntry.getName().substring(pos + inZipDir.length());
        if (filename.isEmpty()) {
            return;
        }

        Path path = toSaveDir.resolve(filename);

        if (zipEntry.isDirectory()) {
            Files.createDirectories(path);
        } else {
            // save file
            byte[] buffer = new byte[2048];
            try (FileOutputStream fos = new FileOutputStream(path.toFile());
                 BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {

                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    /**
     * When an update is available it will also remove the old one and start the download of the new one.
     *
     * @param version
     * @return true when an update is available or the API not existent
     */
    public boolean checkForUpdate(FactorioVersion version) {
        Path luaLibVersionPath = luaLibRootPath.resolve(version.version());
        Path corePrototypeVersionPath = corePrototypeRootPath.resolve(version.version());

        return !luaLibVersionPath.toFile().exists() || !corePrototypeVersionPath.toFile().exists();
    }
}

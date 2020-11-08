package moe.knox.factorio.downloader;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.indexer.BasePrototypesService;
import moe.knox.factorio.library.FactorioLibraryProvider;
import moe.knox.factorio.prototypeDefinition.PrototypeDefinitionService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
final public class DownloaderContainer {
    public static String downloadLink = "https://factorio-api.knox.moe/";
    public static String downloadApiLink = downloadLink + "api/";
    public static String downloadPrototypesJsonLink = downloadLink + "prototypes.json";

    public static String rootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/";
    public static String apiRootPath = rootPath + "api/";
    public static String luaLibRootPath = rootPath + "luaLib/";
    public static String prototypeRootPath = rootPath + "prototypes/";
    public static String prototypeDefinitionLink = rootPath + "prototypes.json";

    private Project project;
    private FactorioAutocompletionState config;
    private ApiDownloader apiDownloader;
    private LuaLibDownloader luaLibDownloader;
    private PrototypeDownloader prototypeDownloader;

    private DownloaderContainer(Project project) {
        apiDownloader = new ApiDownloader(project, "Download Factorio LUA API");
        luaLibDownloader = new LuaLibDownloader(project, "Download LuaLib");
        prototypeDownloader = new PrototypeDownloader(project, "Download prototype definitions");
        config = FactorioAutocompletionState.getInstance(project);
        this.project = project;
    }

    public static synchronized DownloaderContainer getInstance(Project project) {
        return project.getService(DownloaderContainer.class);
    }

    public void updateIfNeeded() {
        String newestAvailableVersion = null;
        try {
            newestAvailableVersion = config.getNewestAvailableVersion();
        } catch (IOException e) {
            e.printStackTrace();
            // TODO show error message
        }

        if (config.downloadedVersion.isEmpty() || !FileUtil.exists(apiRootPath)) {
            download();
            return;
        }

        if (config.selectedVersion.equals("latest")) {
            if (!config.downloadedVersion.equals(newestAvailableVersion)) {
                remove();
                download();
                return;
            }
        }

        // check if update of prototypes is needed
        try {
            URL prototypesJsonVersionUrl = new URL(downloadPrototypesJsonLink + "/version");
            InputStream prototypesJsonVersionStream = prototypesJsonVersionUrl.openStream();
            String prototypeJsonVersion = new String(prototypesJsonVersionStream.readAllBytes());
            if (Long.valueOf(config.downloadedPrototypeTimestamp) < Long.valueOf(prototypeJsonVersion)) {
                prototypeDownloader.cancel();
                Files.delete(Path.of(prototypeDefinitionLink));
                prototypeDownloader.download(null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // TODO show error message
        }
    }

    public void download() {
        String version = config.selectedVersion;
        if (version.equals("latest")) {
            try {
                version = config.getNewestAvailableVersion();
            } catch (IOException e) {
                e.printStackTrace();
                // TODO show error message
            }
        }
        apiDownloader.download(version);
        luaLibDownloader.download(version);
        prototypeDownloader.download(null);

        config.downloadedVersion = version;
    }

    public void remove() {
        // cancel all running downloads
        apiDownloader.cancel();
        luaLibDownloader.cancel();
        prototypeDownloader.cancel();

        // delete apiPath (dont delete rootPath, relevant plugin stuff is in there)
        try {
            FileUtil.delete(Path.of(apiRootPath));
            FileUtil.delete(Path.of(luaLibRootPath));
            FileUtil.delete(Path.of(prototypeRootPath));
            FileUtil.delete(Path.of(prototypeDefinitionLink));
        } catch (IOException e) {
            e.printStackTrace();
            // TODO show error message
        }

        // reload core/base prototypes
        BasePrototypesService.getInstance(project).reloadIndex();
        FactorioLibraryProvider.reload();
        PrototypeDefinitionService.getInstance(project).reload();
    }

    public String getCurrentLuaApiLink() {
        if (config.downloadedVersion.isEmpty() || !FileUtil.exists(apiRootPath)) {
            download();
            return null;
        }

        return apiDownloader.getCurrent(config.downloadedVersion);
    }

    public String getCurrentLuaLibLink() {
        if (config.downloadedVersion.isEmpty() || !FileUtil.exists(luaLibRootPath)) {
            download();
            return null;
        }
        return luaLibDownloader.getCurrentLuaLib(config.downloadedVersion);
    }

    public String getCurrentPrototypeDefinitionLink() {
        if (config.downloadedVersion.isEmpty() || !FileUtil.exists(prototypeRootPath)) {
            download();
            return null;
        }
        return luaLibDownloader.getCurrentPrototype(config.downloadedVersion);
    }

    public String getCurrentPrototypeDefinitionJson() {
        if (!FileUtil.exists(prototypeDefinitionLink)) {
            download();
            return null;
        }
        return prototypeDownloader.getCurrent();
    }
}

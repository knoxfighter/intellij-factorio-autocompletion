package moe.knox.factorio.downloader;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.indexer.BasePrototypesService;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static moe.knox.factorio.downloader.DownloaderContainer.luaLibRootPath;
import static moe.knox.factorio.downloader.DownloaderContainer.prototypeRootPath;

class LuaLibDownloader extends Task.Backgroundable implements Downloader {
    private AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private ProgressIndicator indicator;
    private String version;
    private AtomicBoolean downloadFailed = new AtomicBoolean(false);

    public LuaLibDownloader(@Nullable Project project, @NotNull @NlsContexts.ProgressTitle String title) {
        super(project, title, true);
    }

    @Override
    public void onCancel() {
        super.onCancel();
    }

    private void downloadExtractZip(String version, String luaLibDir, String prototypeDir) {
        // download and extract zipball
        try {
            URL url = new URL("https://api.github.com/repos/wube/factorio-data/zipball/" + version);
            InputStream inputStream = url.openStream();
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);

            Path corePrototypeSubDir = Paths.get(prototypeDir, "core");
            Path basePrototypeSubDir = Paths.get(prototypeDir, "base");

            Files.createDirectories(corePrototypeSubDir);
            Files.createDirectories(basePrototypeSubDir);

            // Iterate over all files in the zip and only save the needed
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                // stop extracting zip, when the execution is cancelled
                if (indicator.isCanceled()) {
                    break;
                }
                saveZipEntry(zipInputStream, zipEntry, "/lualib/", luaLibDir);
                saveZipEntry(zipInputStream, zipEntry, "/core/prototypes/", corePrototypeSubDir.toString());
                saveZipEntry(zipInputStream, zipEntry, "/base/prototypes/", basePrototypeSubDir.toString());
            }
        } catch (IOException e) {
//            e.printStackTrace();
            // TODO print information, that this release has no luaLib tag
            downloadFailed.set(true);
        }
    }

    private void saveZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, String inZipDir, String toSaveDir) {
        int pos = zipEntry.getName().lastIndexOf(inZipDir);
        if (pos > -1) {
            // This thing is inside core-prototype
            String filename = zipEntry.getName().substring(pos + inZipDir.length());
            if (filename.isEmpty()) {
                return;
            }

            Path path = Paths.get(toSaveDir, filename);

            if (zipEntry.isDirectory()) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // save file
                byte[] buffer = new byte[2048];
                try (FileOutputStream fos = new FileOutputStream(path.toFile());
                     BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {

                    int len;
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        bos.write(buffer, 0, len);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        this.indicator = progressIndicator;

        // check if already downloaded
        String luaLibDir = luaLibRootPath + version + "/";
        String prototypePath = prototypeRootPath + version + "/";
        if (!FileUtil.exists(luaLibDir) && !FileUtil.exists(prototypePath)) {
            // create directories
            if (FileUtil.createDirectory(new File(luaLibDir)) && FileUtil.createDirectory(new File(prototypePath))) {
                downloadExtractZip(version, luaLibDir, prototypePath);

                // Reload base prototype service indexes
                ApplicationManager.getApplication().invokeLater(() ->
                        BasePrototypesService.getInstance(myProject).reloadIndex()
                );
            }
        }

        downloadInProgress.set(false);
    }

    @Override
    public void cancel() {
        if (downloadInProgress.get() && !this.indicator.isCanceled() && this.indicator.isRunning()) {
            this.indicator.cancel();
        }
    }

    @Override
    public boolean download(String version) {
        // dont do anything if download in progress
        if (!downloadFailed.get() && downloadInProgress.compareAndSet(false, true)) {
            this.version = version;
            ProgressManager.getInstance().run(this);
            return true;
        } else {
            return false;
        }
    }

    public String getCurrentLuaLib(String version) {
        if (downloadInProgress.get() || (this.indicator != null && (this.indicator.isCanceled() || this.indicator.isRunning())) || downloadFailed.get()) {
            return null;
        }
        return luaLibRootPath + version;
    }

    public String getCurrentPrototype(String version) {
        if (downloadInProgress.get() || (this.indicator != null && (this.indicator.isCanceled() || this.indicator.isRunning())) || downloadFailed.get()) {
            return null;
        }
        return prototypeRootPath + version;
    }
}

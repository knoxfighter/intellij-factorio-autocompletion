package moe.knox.factorio.core;

import com.google.gson.Gson;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import lombok.CustomLog;
import moe.knox.factorio.core.parser.Parser;
import moe.knox.factorio.intellij.FactorioState;
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

@CustomLog
public class LuaLibDownloader extends Parser {
    public static final String luaLibRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/lualib/";
    public static final String prototypeRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/core_prototypes/";
    public static final String lualibGithubTagsLink = "https://api.github.com/repos/wube/factorio-data/git/refs/tags";
    private static final AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private final String saveDir;
    private final String prototypeSaveDir;
    private final FactorioState config;
    private RefTag tag;

    public LuaLibDownloader(@Nullable Project project, String saveDir, String prototypeSaveDir, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        this(project, saveDir, prototypeSaveDir, null, title, canBeCancelled);
    }

    public LuaLibDownloader(@Nullable Project project, String saveDir, String prototypeSaveDir, RefTag tag, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        this.saveDir = saveDir;
        this.prototypeSaveDir = prototypeSaveDir;
        this.tag = tag;
        config = FactorioState.getInstance(project);
    }

    @Nullable
    public static String getCurrentLualibLink(Project project) {
        return getCurrentLink(project, false);
    }

    public static String getCurrentPrototypeLink(Project project) {
        return getCurrentLink(project, true);
    }

    private static String getCurrentLink(Project project, boolean isPrototype) {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioState config = FactorioState.getInstance(project);
        String lualibPath = luaLibRootPath + config.selectedFactorioVersion.version();
        String prototypePath = prototypeRootPath + config.selectedFactorioVersion.version();

        File lualibFile = new File(lualibPath);
        File prototypeFile = new File(prototypePath);
        if (lualibFile.exists() && prototypeFile.exists()) {
            if (isPrototype) {
                return prototypePath;
            } else {
                return lualibPath;
            }
        } else {
            // else request download
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new LuaLibDownloader(project, lualibPath, prototypePath, "Download Factorio Lualib", false));
            }
        }

        return null;
    }

    public static void removeCurrentLualib(Project project) {
        if (!downloadInProgress.get()) {
            FactorioState config = FactorioState.getInstance(project);
            String apiPath = luaLibRootPath;
            FileUtil.delete(new File(apiPath));

            String prototypePath = prototypeRootPath;
            FileUtil.delete(new File(prototypePath));

            PrototypesService.getInstance(project).reloadIndex();
        }
    }

    /**
     * When an update is available it will also remove the old one and start the download of the new one.
     *
     * @param project
     * @return true when an update is available or the API not existent
     */
    public static boolean checkForUpdate(Project project) {
        FactorioState config = FactorioState.getInstance(project);
        String lualibPath = luaLibRootPath + config.selectedFactorioVersion.version();
        String prototypePath = prototypeRootPath + config.selectedFactorioVersion.version();

        File lualibFile = new File(lualibPath);
        File prototypeFile = new File(prototypePath);
        if (lualibFile.exists() && prototypeFile.exists()) {
            if (config.selectedFactorioVersion.latest()) {
                RefTag[] tags = downloadTags();
                if (tags != null) {
                    String tag = tags[tags.length - 1].ref;
                    if (!tag.substring(tag.lastIndexOf("/") + 1).equals(config.currentLualibVersion)) {
                        removeCurrentLualib(project);

                        // download new lualib
                        if (downloadInProgress.compareAndSet(false, true)) {
                            ProgressManager.getInstance().run(new LuaLibDownloader(project, lualibPath, prototypePath, tags[tags.length - 1], "Download Factorio Lualib", false));
                        }

                        return true;
                    }
                } else {
                    NotificationService.getInstance(project).notifyErrorCheckingNewVersion();
                }
            }
            return false;
        } else {
            // api not there, request it...
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new LuaLibDownloader(project, lualibPath, prototypePath, "Download Factorio Lualib", false));
            }
            return true;
        }
    }

    private static RefTag[] downloadTags() {
        RefTag[] tags = null;
        try {
            URL url = new URL(lualibGithubTagsLink);
            InputStreamReader inputStream = new InputStreamReader(url.openStream());
            Gson gson = new Gson();
            tags = gson.fromJson(inputStream, RefTag[].class);
        } catch (IOException e) {
            log.error(e);
        }
        return tags;
    }

    private RefTag getCurrentTag() {
        // request download API
        RefTag[] tags = downloadTags();
        if (tags != null) {
            // find correct Tag
            RefTag correctTag = null;
            if (config.selectedFactorioVersion.latest()) {
                correctTag = tags[tags.length - 1];
            } else {
                for (RefTag tag : tags) {
                    if (tag.ref.substring(tag.ref.lastIndexOf("/") + 1).equals(config.selectedFactorioVersion.version())) {
                        correctTag = tag;
                        break;
                    }
                }
            }

            return correctTag;
        } else {
            NotificationService.getInstance(myProject).notifyErrorDownloadingVersion();
        }

        return null;
    }

    private void downloadExtractZip(RefTag tag) {
        // download and extract zipball
        try {
            String tagName = tag.ref.substring(tag.ref.lastIndexOf("/") + 1);
            URL url = new URL("https://api.github.com/repos/wube/factorio-data/zipball/" + tagName);
            InputStream inputStream = url.openStream();
            try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
                Path corePrototypeSubDir = Paths.get(prototypeSaveDir, "core");
                Path basePrototypeSubDir = Paths.get(prototypeSaveDir, "base");

                Files.createDirectories(corePrototypeSubDir);
                Files.createDirectories(basePrototypeSubDir);

                // Iterate over all files in the zip and only save the needed
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    saveZipEntry(zipInputStream, zipEntry, "/lualib/", saveDir);
                    saveZipEntry(zipInputStream, zipEntry, "/core/prototypes/", corePrototypeSubDir.toString());
                    saveZipEntry(zipInputStream, zipEntry, "/base/prototypes/", basePrototypeSubDir.toString());
                }
            }

            FactorioState.getInstance(myProject).currentLualibVersion = tagName;
        } catch (IOException e) {
            log.error(e);
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
                    log.error(e);
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
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        if (this.tag == null) {
            this.tag = getCurrentTag();
        }

        if (this.tag != null) {
            // create directory where to save to
            Path saveDirPath = Paths.get(saveDir);
            Path prototypeSaveDirPath = Paths.get(prototypeSaveDir);

            try {
                Files.createDirectories(saveDirPath);
                Files.createDirectories(prototypeSaveDirPath);
                downloadExtractZip(this.tag);

                // Reload base prototype service indexes
                ApplicationManager.getApplication().invokeLater(() ->
                        PrototypesService.getInstance(myProject).reloadIndex()
                );
            } catch (IOException e) {
                log.error(e);
//                Notification notification = notificationGroup.createNotification("Error creating Lualib Directory", NotificationType.WARNING);
//                notification.addAction(new NotificationAction("Open Settings") {
//                    @Override
//                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
//                        ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
//                    }
//                });
            }
        } else {
            NotificationService.getInstance(myProject).notifyErrorTagsDownloading();
        }
        downloadInProgress.set(false);
    }

    private class RefTag {
        String ref;
        String mode_id;
        String url;
    }
}

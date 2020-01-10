package moe.knox.factorio.parser;

import com.google.gson.Gson;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.FactorioAutocompletionConfig;
import moe.knox.factorio.FactorioAutocompletionState;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FactorioLualibParser extends FactorioParser {
    private static NotificationGroup notificationGroup = new NotificationGroup("Factorio Lualib Download", NotificationDisplayType.STICKY_BALLOON, true);

    public static final String luaLibRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/lualib/";
    public static final String lualibGithubTagsLink = "https://api.github.com/repos/wube/factorio-data/git/refs/tags";

    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private String saveDir;
    private FactorioAutocompletionState config;
    private RefTag tag;

    public FactorioLualibParser(@Nullable Project project, String saveDir, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        this(project, saveDir, null, title, canBeCancelled);
    }

    public FactorioLualibParser(@Nullable Project project, String saveDir, RefTag tag, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        this.saveDir = saveDir;
        this.tag = tag;
        config = FactorioAutocompletionState.getInstance(project);
    }

    @Nullable
    public static String getCurrentLualibLink(Project project) {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String lualibPath = luaLibRootPath + config.selectedFactorioVersion.link;

        // check if lualib is existant
        File lualibFile = new File(lualibPath);
        if (lualibFile.exists()) {
            return lualibPath;
        } else {
            // else request download
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioLualibParser(project, lualibPath, "Download Factorio Lualib", false));
            }
        }

        return null;
    }

    public static void removeCurrentLualib(Project project) {
        if (!downloadInProgress.get()) {
            FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
            String apiPath = luaLibRootPath;
            FileUtil.delete(new File(apiPath));
        }
    }

    /**
     * When an update is available it will also remove the old one and start the download of the new one.
     *
     * @param project
     * @return true when an update is available or the API not existent
     */
    public static boolean checkForUpdate(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String lualibPath = luaLibRootPath + config.selectedFactorioVersion.link;

        File lualibFile = new File(lualibPath);
        if (lualibFile.exists()) {
            if (config.selectedFactorioVersion.desc.equals("Latest version")) {
                RefTag[] tags = downloadTags();
                if (tags != null) {
                    String tag = tags[tags.length - 1].ref;
                    if (!tag.substring(tag.lastIndexOf("/")).equals(config.currentLualibVersion)) {
                        removeCurrentLualib(project);

                        // download new lualib
                        if (downloadInProgress.compareAndSet(false, true)) {
                            ProgressManager.getInstance().run(new FactorioLualibParser(project, lualibPath, tags[tags.length - 1], "Download Factorio Lualib", false));
                        }

                        return true;
                    }
                } else {
                    // error
                    Notification notification = notificationGroup.createNotification("Error checking new Version. Manual update in the Settings.", NotificationType.WARNING);
                    notification.addAction(new NotificationAction("Open Settings") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                        }
                    });
                }
            }
            return false;
        } else {
            // api not there, request it...
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioLualibParser(project, lualibPath, "Download Factorio Lualib", false));
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
            e.printStackTrace();
        }
        return tags;
    }

    private RefTag getCurrentTag() {
        // request download API
        RefTag[] tags = downloadTags();
        if (tags != null) {
            // find correct Tag
            RefTag correctTag = null;
            if (config.selectedFactorioVersion.desc.equals("Latest version")) {
                correctTag = tags[tags.length - 1];
            } else {
                for (RefTag tag : tags) {
                    if (tag.ref.substring(tag.ref.lastIndexOf("/")).equals(config.selectedFactorioVersion.desc)) {
                        correctTag = tag;
                        break;
                    }
                }
            }

            return correctTag;
        } else {
            Notification notification = notificationGroup.createNotification("Error downloading Version overview", NotificationType.WARNING);
            notification.addAction(new NotificationAction("Open Settings") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
                }
            });
        }

        return null;
    }

    private void downloadExtractZip(RefTag tag) {
        // download and extract zipball
        try {
            String tagName = tag.ref.substring(tag.ref.lastIndexOf("/") + 1);
            URL url = new URL("https://api.github.com/repos/wube/factorio-data/zipball/" + tagName);
            InputStream inputStream = url.openStream();
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);

            byte[] buffer = new byte[2048];

            // Iterate over all files in the zip and only save the needed
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                int lualibPos = zipEntry.getName().lastIndexOf("/lualib/");
                if (lualibPos > -1) {
                    // This is a thing inside lualib dir
                    String filename = zipEntry.getName().substring(lualibPos + "/lualib/".length());
                    if (filename.isEmpty()) {
                        continue;
                    }
                    Path path = Paths.get(saveDir, filename);

                    if (zipEntry.isDirectory()) {
                        // create directory
                        path.toFile().mkdir();
                    } else {
                        // save file
                        try (FileOutputStream fos = new FileOutputStream(path.toFile());
                             BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {

                            int len;
                            while ((len = zipInputStream.read(buffer)) > 0) {
                                bos.write(buffer, 0, len);
                            }
                        }
                    }
                }
            }

            FactorioAutocompletionState.getInstance(myProject).currentLualibVersion = tagName;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        if (this.tag == null) {
            this.tag = getCurrentTag();
        }

        if (this.tag != null) {
            // create directory where to save to
            File file = new File(saveDir);
            if (file.exists() || file.mkdirs()) {
                downloadExtractZip(this.tag);
            } else {
                Notification notification = notificationGroup.createNotification("Error creating Lualib Directory", NotificationType.WARNING);
                notification.addAction(new NotificationAction("Open Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
                    }
                });
            }
        } else {
            Notification notification = notificationGroup.createNotification("Error getting current tags. Lualib not downloaded.", NotificationType.WARNING);
            notification.addAction(new NotificationAction("Open Settings") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
                }
            });
        }
        downloadInProgress.set(false);
    }

    private class RefTag {
        String ref;
        String mode_id;
        String url;
    }
}

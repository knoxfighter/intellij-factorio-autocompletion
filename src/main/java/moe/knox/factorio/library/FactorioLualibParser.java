package moe.knox.factorio.library;

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
    public static final String lualibGithubTagsLink = "https://api.github.com/repos/wube/factorio-data/tags";

    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private String saveDir;
    private Tag tag;

    public FactorioLualibParser(@Nullable Project project, String saveDir, Tag tag, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        super(project, title);
        this.saveDir = saveDir;
        this.tag = tag;
    }

    public static String getCurrentLualibLink(Project project) {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String lualibPath = luaLibRootPath + config.selectedFactorioVersion.link;

        File lualibFile = new File(lualibPath);
        if (lualibFile.exists()) {
            if (config.selectedFactorioVersion.desc.equals("Latest version")) {
                Tag[] tags = downloadTags();
                if (tags != null) {
                    if (!tags[0].name.equals(config.currentLualibVersion)) {
                        removeCurrentLualib(project);

                        if (!downloadInProgress.get()) {
                            downloadInProgress.set(true);
                            ProgressManager.getInstance().run(new FactorioLualibParser(project, lualibPath, tags[0], "Download Factorio Lualib", false));
                        }

                        return null;
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
            return lualibPath;
        } else {
            // create dir
            if (lualibFile.mkdirs()) {
                // request download API
                Tag[] tags = downloadTags();
                if (tags != null) {
                    // find correct Tag
                    Tag correctTag = null;
                    if (config.selectedFactorioVersion.desc.equals("Latest version")) {
                        correctTag = tags[0];
                    } else {
                        for (Tag tag : tags) {
                            if (tag.name == config.selectedFactorioVersion.desc) {
                                correctTag = tag;
                            }
                        }
                    }

                    // only run, when Tag is found, when not, no factorio-data is available
                    if (correctTag != null && !downloadInProgress.get()) {
                        downloadInProgress.set(true);
                        ProgressManager.getInstance().run(new FactorioLualibParser(project, lualibPath, correctTag, "Download and Parse Factorio API", false));
                    }
                } else {
                    Notification notification = notificationGroup.createNotification("Error downloading Version overview", NotificationType.WARNING);
                    notification.addAction(new NotificationAction("Open Settings") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                        }
                    });
                }
            } else {
                Notification notification = notificationGroup.createNotification("Error creating Lualib Directory", NotificationType.WARNING);
                notification.addAction(new NotificationAction("Open Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                    }
                });
            }
        }

        return null;
    }

    private static void removeCurrentLualib(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = luaLibRootPath + config.selectedFactorioVersion.link;
        FileUtil.delete(new File(apiPath));
    }

    private static Tag[] downloadTags() {
        Tag[] tags = null;
        try {
            URL url = new URL(lualibGithubTagsLink);
            InputStreamReader inputStream = new InputStreamReader(url.openStream());
            Gson gson = new Gson();
            tags = gson.fromJson(inputStream, Tag[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tags;
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        // download and extract zipball
        try {
            URL url = new URL(tag.zipball_url);
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

            FactorioAutocompletionState.getInstance(myProject).currentLualibVersion = tag.name;
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadInProgress.set(false);
    }

    /**
     * This is the json Design of the tags from github
     */
    private class Tag {
        String name;
        String zipball_url;
        String tarball_url;
        Commit commit;
        String node_id;
    }

    private class Commit {
        String sha;
        String url;
    }
}

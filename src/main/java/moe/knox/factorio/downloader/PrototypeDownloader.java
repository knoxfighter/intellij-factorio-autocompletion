package moe.knox.factorio.downloader;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.FactorioAutocompletionConfig;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.prototypeDefinition.PrototypeDefinitionService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static moe.knox.factorio.downloader.DownloaderContainer.*;

public class PrototypeDownloader extends Task.Backgroundable implements Downloader {
    protected static NotificationGroup notificationGroup = new NotificationGroup("Prototype.json Download", NotificationDisplayType.STICKY_BALLOON, true);
    private AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private ProgressIndicator indicator;

    public PrototypeDownloader(@Nullable Project project, @NotNull @NlsContexts.ProgressTitle String title) {
        super(project, title, true);
    }

    @Override
    public boolean download(@Nullable String version) {
        if (downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(this);
            return true;
        } else {
            return false;
        }
    }

    public String getCurrent() {
        if (downloadInProgress.get() || (this.indicator != null && (this.indicator.isCanceled() || this.indicator.isRunning()))) {
            return null;
        }
        return prototypeDefinitionPath;
    }

    @Override
    public void cancel() {
        if (downloadInProgress.get() && (this.indicator != null && (!this.indicator.isCanceled() && this.indicator.isRunning()))) {
            this.indicator.cancel();
        }
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        this.indicator = progressIndicator;

        // check if already downloaded
        if (!FileUtil.exists(prototypeDefinitionPath)) {
            try {
                FileUtil.createDirectory(new File(prototypeDefinitionPath));

                // download prototypes.json
                URL prototypesJsonUrl = new URL(downloadPrototypesJsonLink);
                InputStream prototypesJsonStream = prototypesJsonUrl.openStream();

                // save the file
                Files.copy(prototypesJsonStream, Paths.get(prototypeJsonLink));
                prototypesJsonStream.close();

                // download prototypes.lua
                URL prototypesLuaUrl = new URL(downloadPrototypesLuaLink);
                InputStream prototypesLuaStream = prototypesLuaUrl.openStream();

                // save the file
                Files.copy(prototypesLuaStream, Paths.get(prototypeLuaLink));
                prototypesLuaStream.close();

                // get current prototype.json version
                URL prototypesJsonVersionUrl = new URL(downloadPrototypesJsonLink + "/version");
                InputStream prototypesJsonVersionStream = prototypesJsonVersionUrl.openStream();
                FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(myProject);
                config.downloadedPrototypeTimestamp = new String(prototypesJsonVersionStream.readAllBytes());

                // reload prototype definitions
                PrototypeDefinitionService.getInstance(myProject).reload();
            } catch (IOException e) {
                if (!indicator.isCanceled()) {
                    e.printStackTrace();
                    printMessage("Error downloading the prototypes.json. Please go online and try it again!\n" +
                            "Integration is partially disabled until reloaded in Settings.");
                }
            }
        }
        downloadInProgress.set(false);
    }

    private void printMessage(String message) {
        Notification notification = notificationGroup.createNotification(
                message,
                NotificationType.ERROR
        );
        notification.addAction(new NotificationAction("Open settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
            }
        });
        Notifications.Bus.notify(notification, myProject);
    }
}

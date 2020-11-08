package moe.knox.factorio;

import com.google.gson.Gson;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import moe.knox.factorio.downloader.DownloaderContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

@State(
        name = "FactorioAutocompletionConfig",
        storages = {
                @Storage("FactorioAutocompletionConfig.xml")
        }
)
public class FactorioAutocompletionState implements PersistentStateComponent<FactorioAutocompletionState> {
    public boolean integrationActive = false;
    public String curVersion = "";

    public String selectedVersion = "";
    public String downloadedVersion = "";
    public String downloadedPrototypeTimestamp = "";
    private String newestAvailableVersion = "";
    private String[] availableVersions;
    private Project project;

    public void reloadAvailableVersions() throws IOException {
        // load all available versions
        URL apiLinkURL = new URL(DownloaderContainer.downloadApiLink);
        InputStream apiLinkStream = apiLinkURL.openStream();
        availableVersions = new Gson().fromJson(new InputStreamReader(apiLinkStream), String[].class);
        newestAvailableVersion = availableVersions[availableVersions.length - 1];
    }

    public String[] getAvailableVersions() throws IOException {
        if (availableVersions == null || availableVersions.length == 0) {
            reloadAvailableVersions();
        }
        return availableVersions;
    }

    public String getNewestAvailableVersion() throws IOException {
        if (availableVersions == null || availableVersions.length == 0) {
            reloadAvailableVersions();
        }
        return newestAvailableVersion;
    }

    @Nullable
    @Override
    public FactorioAutocompletionState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FactorioAutocompletionState state) {
        XmlSerializerUtil.copyBean(state, this);

        if (!this.curVersion.isEmpty()) {
            this.selectedVersion = this.curVersion;
            this.curVersion = "";

            NotificationGroup notificationGroup = new NotificationGroup("Factorio autocompletion changed", NotificationDisplayType.STICKY_BALLOON, true);
            Notification notification = notificationGroup.createNotification(
                    "A new version of the factorio autocompletion plugin has been loaded. " +
                            "The settings got changed, so the latest downloaded version of the API will be set as currently selected version. " +
                            "You can load a different version in the settings (also a \"current\" setting is available).",
                    NotificationType.INFORMATION
            );
            notification.addAction(new NotificationAction("Open settings") {
                @Override
                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                }
            });
            Notifications.Bus.notify(notification, project);
        }
    }

    public static FactorioAutocompletionState getInstance(Project project) {
        FactorioAutocompletionState service = ServiceManager.getService(project, FactorioAutocompletionState.class);
        service.project = project;
        return service;
    }
}

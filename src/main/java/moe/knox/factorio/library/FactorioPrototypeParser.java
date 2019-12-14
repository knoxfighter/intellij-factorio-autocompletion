package moe.knox.factorio.library;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioPrototypeParser extends FactorioParser {
    private static NotificationGroup notificationGroup = new NotificationGroup("Factorio Prototype Download", NotificationDisplayType.STICKY_BALLOON, true);

    public static String prototypeRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_prototypes/";
    public static String prototypesBaseLink = "https://wiki.factorio.com";

    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;

    public FactorioPrototypeParser(@Nullable Project project, String saveDir, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title) {
        super(project, title, false);
        this.saveDir = saveDir;
    }

    public static String getCurrentPrototypeLink(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);

        // check if prototypes are downloaded
        File protoPathFile = new File(prototypeRootPath);
        if (protoPathFile.exists()) {
            return prototypeRootPath;
        } else {
            // request download API
            if (!downloadInProgress.get()) {
                downloadInProgress.set(true);
                ProgressManager.getInstance().run(new FactorioPrototypeParser(project, prototypeRootPath, "Download and Parse Factorio Prototypes"));
            }
            return null;
        }
    }

    public static void removeCurrentPrototypes() {
        String apiPath = prototypeRootPath;
        FileUtil.delete(new File(apiPath));
    }

    @Override
    public void run(@NotNull ProgressIndicator progressIndicator) {
        this.indicator = progressIndicator;
        this.config = FactorioAutocompletionState.getInstance(myProject);

        // start the whole thing
        assureDir();

        downloadInProgress.set(false);

        // whole thing finished, reload the Library-Provider
        ApplicationManager.getApplication().invokeLater(() -> FactorioLibraryProvider.reload());
    }

    /**
     * Entry-point with creating the used directory
     * It will assure, that the directory is there and will start the downloading and parsing.
     */
    private void assureDir() {
        File dirFile = new File(saveDir);
        if (!dirFile.exists()) {
            // file does not exist ... create it
            if (dirFile.mkdirs()) {
                // download and parse API
                downloadAndParsePrototypes();
                return;
            }
        }
        Notification notification = notificationGroup.createNotification("Error creating the directories for the Factorio Prototypes.", NotificationType.ERROR);
        notification.addAction(new NotificationAction("Open Settings") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
            }
        });
        Notifications.Bus.notify(notification, myProject);
    }

    private double curTodo = 0;
    private double maxTodo = 0;

    private void updateIndicator() {
        indicator.setFraction(curTodo / maxTodo);
        curTodo++;
    }

    /**
     * Entry-point for parsing
     * Download the main Prototype-Page and parse all prototypes
     */
    private void downloadAndParsePrototypes() {
        indicator.setIndeterminate(false);

        String prototypesOverviewLink = prototypesBaseLink + "/Prototype_definitions";
        Document protoOverview;
        try {
            protoOverview = Jsoup.connect(prototypesOverviewLink).get();
        } catch (IOException e) {
            System.out.println("error downloading the main Prototype page");
            showDownloadingError(false);
            return;
        }

        // get links to prototypes
        Elements prototypeElements = protoOverview.select("#mw-content-text ul a");
        maxTodo = prototypeElements.size();
        updateIndicator();

        for (Element prototypeElement : prototypeElements) {
            Prototype prototype = new Prototype();
            if(!prototype.parsePrototype(prototypeElement.attr("href"))) {
                break;
            }
            updateIndicator();
        }
    }

    private class Prototype {
        String name;
        String id;
        String parentType;
        List<String> description = new ArrayList<>();
        List<Property> properties = new ArrayList<>();

        private boolean parsePrototype(String link) {
            String prototypeLink = prototypesBaseLink + link;
            Document prototypeDoc;
            try {
                prototypeDoc = Jsoup.connect(prototypeLink).get();
            } catch (IOException e) {
                System.out.printf("error downloading the single prototype page: %s", link);
                showDownloadingError(false);
                return false;
            }

            Element prototypeNameElement = prototypeDoc.selectFirst("td.caption");
            name = prototypeNameElement.text().split("—")[0].strip().replace("/", "_");
            id = prototypeNameElement.selectFirst("code").text();

            // parse parentType
            Element parentTypeElement = prototypeDoc.selectFirst(".prototype-toc td.prototype-toc-section-title a");
            if (parentTypeElement != null) {
                this.parentType = parentTypeElement.attr("title").strip().replace("/", "_");
            }

            // parse elements
            Element element = prototypeDoc.selectFirst(".prototype-parents");

            boolean atProperties = false;
            Property property = null;
            boolean propertyFirst = false;

            while (true) {
                element = element.nextElementSibling();
                if (element == null) {
                    break;
                } else if (element.hasClass("prototype-toc")) {
                    atProperties = true;
                } else if (element.is("h3") && atProperties) {
                    Element spanChild = element.selectFirst("span");
                    property = new Property(spanChild.attr("id"));
                    propertyFirst = true;
                    properties.add(property);
                } else if (element.is("p") && atProperties && propertyFirst) {
                    propertyFirst = false;

                    for (Element child : element.children()) {
                        if (child.is("a")) {
                            if (property != null) {
                                property.type = element.selectFirst("a").text();
                            }
                        }
                        if (child.is("b") && child.text().equals("Default")) {
                            property.optional = true;
                        }
                        child.remove();
                    }
                    property.description.add(removeNewLines(element.text().strip()).strip().replaceAll("^[:]+|[:]+$", "").strip());
                } else if (element.is("p") && atProperties && !propertyFirst && property != null) {
                    if (property == null) {
                        System.out.println("kuckuck");
                    }
                    property.description.add(element.text());
                } else if (element.is("p") && !atProperties && property != null){
                    description.add(element.text());
                }
            }

            saveToFile();

            return true;
        }

        public void saveToFile() {
            // create new file content
            StringBuilder typeFileContent = new StringBuilder();

            for (String s : this.description) {
                typeFileContent.append("---").append(s).append(newLine);
            }

            typeFileContent.append("---@class ").append(this.name);
            if (this.parentType != null && !this.parentType.isEmpty()) {
                typeFileContent.append(" : ").append(this.parentType);
            }
            typeFileContent.append(newLine);
            typeFileContent.append(this.name).append(" = {}").append(newLine).append(newLine);

            // add all properties
            for (Property property : properties) {
                for (String s : property.description) {
                    typeFileContent.append("--- ").append(s).append(newLine);
                }

                // add type
                typeFileContent.append("---@type ").append(property.type).append(newLine);

                // add attribute definition
                typeFileContent.append(this.name).append(".").append(property.name).append(" = nil").append(newLine).append(newLine).append(newLine);
            }

            // create file
            String typeDir = saveDir + this.name + ".lua";
            saveStringToFile(typeDir, typeFileContent.toString());
        }
    }

    private class Property {
        String name;
        String type;
        List<String> description = new ArrayList<>();
        boolean optional = false;

        public Property(String name) {
            this.name = name;
        }
    }
}

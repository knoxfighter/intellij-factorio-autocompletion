package moe.knox.factorio.parser;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.tang.intellij.lua.search.SearchContext;
import moe.knox.factorio.FactorioAutocompletionConfig;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.FactorioPrototypeState;
import moe.knox.factorio.indexer.BasePrototypesService;
import moe.knox.factorio.library.FactorioLibraryProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioPrototypeParser extends FactorioParser {
    private static NotificationGroup notificationGroup = new NotificationGroup("Factorio Prototype Download", NotificationDisplayType.STICKY_BALLOON, true);

    public static final String prototypeRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_prototypes/";
    private static final String prototypeLibPath = prototypeRootPath + "library/";
    public static final String prototypesBaseLink = "https://wiki.factorio.com";

    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;

    private static List<String> rootTypes = new ArrayList<>() {{
        add("float");
        add("double");
        add("int");
        add("int8");
        add("int16");
        add("int32");
        add("int64");
        add("uint");
        add("uint8");
        add("uint16");
        add("uint32");
        add("uint64");
        add("string");
        add("LocalisedString");
        add("bool");
    }};

    private static List<String> prototypeTypeWhitelist = new ArrayList<>() {{
        add("Types/ItemProductPrototype");
        add("Types/ItemToPlace");
        add("Types/DamagePrototype");
    }};

    /**
     * map of all propertyTypes `typeName` > `link`
     */
    private Map<String, String> propertyTypes = new HashMap<>();


    public FactorioPrototypeParser(@Nullable Project project, String saveDir, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title) {
        super(project, title, false);
        this.saveDir = saveDir;
    }

    public static String getCurrentPrototypeLink(Project project) {
        // return null if download is in progress
        if (downloadInProgress.get()) {
            return null;
        }

        // check if prototypes are downloaded
        File protoPathFile = new File(prototypeLibPath);
        if (protoPathFile.exists()) {
            return prototypeLibPath;
        } else {
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioPrototypeParser(project, prototypeLibPath, "Download and Parse Factorio Prototypes"));
            }
            return null;
        }
    }

    public static void removeCurrentPrototypes() {
        if (!downloadInProgress.get()) {
            String apiPath = prototypeRootPath;
            FileUtil.delete(new File(apiPath));
            FactorioLibraryProvider.reload();
        }
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
            } else {
                Notification notification = notificationGroup.createNotification("Error creating the directories for the Factorio Prototypes.", NotificationType.ERROR);
                notification.addAction(new NotificationAction("Open Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(myProject, FactorioAutocompletionConfig.class);
                    }
                });
                Notifications.Bus.notify(notification, myProject);
            }
        }
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

        List<String> prototypeIds = new ArrayList<>();

        for (Element prototypeElement : prototypeElements) {
            Prototype prototype = new Prototype(true);
            if (!prototype.parsePrototype(prototypeElement.attr("href"))) {
                break;
            }
            if (prototype.id != null && !prototype.id.equals("abstract")) {
                prototypeIds.add(prototype.id);
            }
            updateIndicator();
        }

        FactorioPrototypeState.getInstance().setPrototypeTypes(prototypeIds);

        // update indicator max value
        maxTodo += propertyTypes.size();

        SearchContext searchContext = SearchContext.Companion.get(getProject());

        propertyTypes.forEach((typeName, typeLink) -> {
            if (!rootTypes.contains(typeName)) {
                // parse additional types, like normal Prototypes
                Prototype prototype = new Prototype();
                prototype.name = typeName;

                // All unsuccessful types are hardcoded in the lua library
                prototype.parsePrototype(typeLink);
            }
            updateIndicator();
        });
    }

    private class Prototype {
        String name;
        String id;
        String parentType;
        boolean catchTypes = false;
        List<String> description = new ArrayList<>();
        List<Property> properties = new ArrayList<>();

        public Prototype() {

        }

        public Prototype(boolean catchTypes) {
            this.catchTypes = catchTypes;
        }

        /**
         * Parse the html page of a single prototype
         *
         * @param link Link to the prototype page
         * @return true if the parsing was successful
         */
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
            if (prototypeNameElement != null) {
                name = prototypeNameElement.text().split("—")[0].strip().replace("/", "_");
                id = prototypeNameElement.selectFirst("code").text();
            }

            // parse parentType
            Element parentTypeElement = prototypeDoc.selectFirst(".prototype-toc td.prototype-toc-section-title a");
            if (parentTypeElement != null) {
                this.parentType = parentTypeElement.attr("title").strip().replace("/", "_");
            }

            // parse elements
            boolean atProperties = false;
            Property property = null;
            boolean propertyFirst = false;
            boolean isInlineType = false;

            // get the element where to start from
            Element element = prototypeDoc.selectFirst(".prototype-parents");
            if (element == null) {
                element = prototypeDoc.selectFirst("#toc");
                atProperties = true;
            }

            if (element == null) {
                element = prototypeDoc.selectFirst("#firstHeading");
                if (element != null && prototypeTypeWhitelist.contains(element.text())) {
                    element = prototypeDoc.selectFirst("#mw-content-text > .mw-parser-output > p:first-of-type");
                } else {
                    return false;
                }
            }

            while (true) {
                element = element.nextElementSibling();

                if (element == null) {
                    // no element anymore, cancel endless loop
                    break;
                } else if (element.hasClass("prototype-toc")) {
                    // from here, the properties are starting
                    atProperties = true;
                } else if (element.is("h3") && atProperties) {
                    // parse the header and create a property
                    Element spanChild = element.selectFirst("span");
                    property = new Property(spanChild.attr("id"));
                    propertyFirst = true;
                    properties.add(property);
                } else if (element.is("p") && atProperties && propertyFirst) {
                    // Parse the properties description and more
                    propertyFirst = false;
                    boolean firstLink = true;
                    boolean isArray = false;

                    String elementHtml = element.html();
                    String[] splittedElementHtml = elementHtml.split("<br>");
                    for (int i = 0; i < splittedElementHtml.length; i++) {
                        if (i == 0) {
                            // First Element is always the type
                            // check if first element is really the type!!
                            String[] splittedType = splittedElementHtml[i].split(":");
                            if (!Jsoup.clean(splittedType[0], Whitelist.none()).equals("Type")) {
                                break;
                            }
                            String elementType = splittedType[1];
                            Document typeDocument = Jsoup.parseBodyFragment(elementType);

                            if (typeDocument.text().startsWith("table of ")) {
                                isArray = true;
                            }

                            // The last link is used to determine which Type it is
                            Elements links = typeDocument.select("a");
                            if (links.size() == 0) {
                                continue;
                            }
                            Element lastLink = links.last();
                            property.type = lastLink.text();
                            property.type = property.type.replace("/Types/", "").replace("Types/", "");

                            // When type is table, the type is defined inline
                            if (property.type.equals("table") || property.type.equals("tables")) {
                                isInlineType = true;
                                property.type = "Type_" + this.name + "_" + property.name;
                            } else if (catchTypes) {
                                propertyTypes.put(property.type, lastLink.attr("href"));
                            }
                        } else {
                            // The rest
                            Document document = Jsoup.parseBodyFragment(splittedElementHtml[i]);
                            Element body = document.body();
                            property.description.add(removeNewLines(document.text().strip()).strip().replaceAll("^[:]+|[:]+$", "").strip());
                        }
                    }
                    if (isArray) {
                        property.type += "[]";
                    }
                } else if (element.is("p") && atProperties && !propertyFirst && property != null) {
                    // parse general property description
                    property.description.add(element.text());
                } else if (element.is("p") && !atProperties) {
                    // parse general description
                    description.add(element.text());
                } else if (element.is("ul") && isInlineType) {
                    // parse inline type
                    parseInlineType(element, property);
                } else if (element.is("h2")) {
                    // check if this header describes the "Differing defaults" category
                    // stop here, nothing useful will come further
                    if (element.selectFirst("span").id().equals("Differing_defaults")) {
                        break;
                    }
                }
            }

            saveToFile();

            return true;
        }

        private void parseInlineType(@NotNull Element element, @NotNull Property property) {
            Elements tableElements = element.children();

            // tableElement is the <li> html element
            for (Element tableElement : tableElements) {
                // parse the <li> text
                String elementComplete = tableElement.text();
                String[] elementParts = elementComplete.split("\n")[0].split("-");

                Property elemProperty = new Property();

                for (int i = 0; i < elementParts.length; i++) {
                    String elementPart = elementParts[i].strip();
                    boolean breaker = false;
                    switch (i) {
                        case 0:
                            elemProperty.name = elementPart;
                            break;
                        case 1:
                            // This inlineType has its type defined inline!
                            if (elementPart.startsWith("table")) {
                                breaker = true;
                                Element subElement = tableElement.selectFirst("ul");
                                elemProperty.type = property.type + "_" + elemProperty.name;
                                if (subElement != null) {
                                    parseInlineType(subElement, elemProperty);
                                }
                            } else {
                                elemProperty.type = elementPart;
                            }
                            break;
                        case 2:
                            if (elementPart.equals("Optional.")) {
                                elemProperty.optional = true;
                                break;
                            } else if (elementPart.equals("Mandatory.")) {
                                break;
                            }
                        default:
                            elemProperty.description.add(elementPart);
                            break;
                    }
                    if (breaker) {
                        break;
                    }
                }
                property.inlineType.properties.add(elemProperty);
            }
        }

        /**
         * Save this prototype-class to a file, so it is accessible
         */
        public void saveToFile() {
            // create new file content
            StringBuilder typeFileContent = new StringBuilder();

            saveTable(typeFileContent, this.description, this.name, this.parentType, this.properties);

            // create file
            String typeDir = saveDir + this.name + ".lua";
            saveStringToFile(typeDir, typeFileContent.toString());
        }

        private void saveTable(StringBuilder fileContent, List<String> description, String name, @Nullable String parentType, List<Property> properties) {
            for (String s : description) {
                fileContent.append("---").append(s).append(newLine);
            }

            fileContent.append("---@class ").append(name);
            if (parentType != null && !parentType.isEmpty()) {
                fileContent.append(" : ").append(parentType);
            }
            fileContent.append(newLine);
            fileContent.append("local ").append(name).append(" = {}").append(newLine).append(newLine);

            // add all properties
            for (Property property : properties) {
                for (String s : property.description) {
                    fileContent.append("--- ").append(s).append(newLine);
                }

                // add type
                fileContent.append("---@type ").append(property.type).append(newLine);

                // add attribute definition
                fileContent.append(name).append(".").append(property.name).append(" = nil").append(newLine).append(newLine);

                if (property.inlineType.properties.size() > 0) {
                    saveTable(fileContent, new ArrayList<>(), property.type, null, property.inlineType.properties);
                }
            }
        }
    }

    private class Property {
        String name;
        String type;
        SubPrototype inlineType = new SubPrototype();
        List<String> description = new ArrayList<>();
        boolean optional = false;

        public Property() {

        }

        public Property(String name) {
            this.name = name;
        }
    }

    private class SubPrototype {
        List<Property> properties = new ArrayList<>();
    }
}

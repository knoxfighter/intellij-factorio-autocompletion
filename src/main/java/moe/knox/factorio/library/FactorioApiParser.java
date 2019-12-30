package moe.knox.factorio.library;

import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioApiParser extends FactorioParser {
    public static String apiRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_api/";
    public static String factorioApiBaseLink = "https://lua-api.factorio.com/";

    private static NotificationGroup notificationGroup = new NotificationGroup("Factorio API Download", NotificationDisplayType.STICKY_BALLOON, true);
    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;

    public FactorioApiParser(@Nullable Project project, String saveDir, @Nls(capitalization = Nls.Capitalization.Title) @NotNull String title, boolean canBeCancelled) {
        super(project, title, canBeCancelled);
        this.saveDir = saveDir;
    }

    /**
     * get the current API Link. If the API is not there, the download will be started in a background thread.
     * When the download is in progress, this function returns instantly null.
     *
     * @param project
     * @return the path to the API or null
     */
    @Nullable
    public static String getCurrentApiLink(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = apiRootPath + config.selectedFactorioVersion.link;

        // check if API is downloaded
        File apiPathFile = new File(apiPath);
        if (apiPathFile.exists()) {
            if (config.selectedFactorioVersion.desc.equals("Latest version")) {
                Document doc = null;
                try {
                    doc = Jsoup.connect("https://lua-api.factorio.com/").get();
                } catch (IOException e) {
//                    e.printStackTrace();
                    Notification notification = notificationGroup.createNotification("Error checking new Version. Manual update in the Settings.", NotificationType.WARNING);
                    notification.addAction(new NotificationAction("Open Settings") {
                        @Override
                        public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                        }
                    });
                    return apiPath;
                }
                if (!doc.select("a").get(1).text().equals(config.curVersion)) {
                    // new version detected, update it
                    removeCurrentAPI(project);
                    if (!downloadInProgress.get()) {
                        downloadInProgress.set(true);
                        ProgressManager.getInstance().run(new FactorioApiParser(project, apiPath, "Download and Parse Factorio API", false));
                    }
                    return null;
                }
            }
            return apiPath;
        } else {
            // request download API
            if (!downloadInProgress.get()) {
                downloadInProgress.set(true);
                ProgressManager.getInstance().run(new FactorioApiParser(project, apiPath, "Download and Parse Factorio API", false));
            }
            return null;
        }
    }

    public static void removeCurrentAPI(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = apiRootPath + config.selectedFactorioVersion.link;
        FileUtil.delete(new File(apiPath));
    }

    /**
     * Entry-point for the Task.Backgroundable.
     * This is the basic entrypoint for the ProgressManager started Thread.
     * After the download is finished, the FactorioLibraryProvider is reloaded.
     *
     * @param indicator
     */
    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        this.indicator = indicator;
        config = FactorioAutocompletionState.getInstance(myProject);

        // start the whole thing
        assureDir();

        downloadInProgress.set(false);

        // whole thing finished, reload the Library-Provider
        ApplicationManager.getApplication().invokeLater(() -> FactorioLibraryProvider.reload());
    }

    public class Attribute {
        public String name;
        public String type;
        public List<String> desc = new ArrayList<>();
        public boolean read;
        public boolean write;
        public List<Parameter> returnParameters = new ArrayList<>();

        public Attribute() {
        }

        /**
         * Parse an Attribute of a factorio class.
         * Will also parse additional Parameters, if the return type is a table.
         * Additional class will be created for that return type, so it is correctly autocomplete
         *
         * @param attributeElement Element, that contains the attribute
         */
        private void parse(@NotNull Element attributeElement) {
            Element header = attributeElement.selectFirst(".element-header");

            // Get type of this attribute
            Element paramType = header.selectFirst(".attribute-type .param-type");
            this.type = parseType(paramType);

            // Get read and write permissions
            Element attributeMode = header.selectFirst(".attribute-mode");
            String modeText = attributeMode.text();
            modeText = modeText.replace("[", "").replace("]", "");
            String[] modes = modeText.split("-");
            for (String mode : modes) {
                if (mode.equals("Read")) {
                    this.read = true;
                } else if (mode.equals("Write")) {
                    this.write = true;
                }
            }

            // get description
            Element attributeContent = attributeElement.selectFirst(".element-content");
            for (Element attributeContentChild : attributeContent.children()) {
                String childHtml = attributeContentChild.html().strip();
                if (!childHtml.isEmpty()) {
                    this.desc.add(removeNewLines(childHtml));
                }
            }

            Element tableFieldList = attributeContent.selectFirst(".field-list");

            if (tableFieldList != null) {
                for (Element parameterElement : tableFieldList.children()) {
                    Parameter parameter = new Parameter();
                    if (parameter.parseDefault(parameterElement)) {
                        returnParameters.add(parameter);
                    }
                }
            }
        }
    }

    public class Method {
        public String name;
        public String returnType;
        public String returnTypeDesc;
        public List<String> desc = new ArrayList<>();
        public List<Parameter> parameters = new ArrayList<>();
        private FactorioType factorioType;

        public Method(FactorioType factorioType) {
            this.factorioType = factorioType;
        }

        /**
         * Copy-Constructor
         *
         * @param other copied Method
         */
        Method(@NotNull Method other) {
            this.name = other.name;
            this.returnType = other.returnType;
            this.returnTypeDesc = other.returnTypeDesc;
            this.factorioType = other.factorioType;

            this.desc.addAll(other.desc);
            for (Parameter parameter : other.parameters) {
                this.parameters.add(new Parameter(parameter));
            }
        }

        /**
         * Parse Method of a factorio class.
         * This will start the parsing of the Parameters.
         * When finding optional Parameters, the Method gets copied, to have an overload without optionals.
         *
         * @param methodNameElement
         * @param methodElement
         */
        private void parseSimple(@NotNull Element methodNameElement, @NotNull Element methodElement) {
            // get return type
            Element returnTypeElement = methodNameElement.selectFirst(".return-type .param-type");
            if (returnTypeElement != null) {
                this.returnType = parseType(returnTypeElement);
            }

            // get short description
            Element methodElementContent = methodElement.selectFirst(".element-content");
            for (Element methodElementContentChild : methodElementContent.children()) {
                if (!methodElementContentChild.hasClass("detail") && !methodElementContentChild.hasClass("notes")) {
                    String text = removeNewLines(methodElementContentChild.html());
                    if (!text.isEmpty()) {
                        this.desc.add(text);
                    }
                }
            }

            // add notes to description
            Elements methodNotes = methodElementContent.select(".note");
            for (Element methodNote : methodNotes) {
                this.desc.add(removeNewLines(methodNote.html()));
            }

            // get Parameters
            Elements methodElementContentDetails = methodElementContent.select(".detail");
            ListIterator<Element> methodElementContentDetailsIterator = methodElementContentDetails.listIterator(methodElementContentDetails.size());

            // Iterate backwards, so return value is parsed first
            while (methodElementContentDetailsIterator.hasPrevious()) {
                Element methodDetail = methodElementContentDetailsIterator.previous();
                Element header = methodDetail.selectFirst(".detail-header");

                // If header not present, add content to description
                if (header == null) {
                    String detailHtml = removeNewLines(methodDetail.html());
                    this.desc.add(detailHtml);
                    continue;
                }

                // Parse Parameters
                if (header.text().equals("Parameters")) {
                    Element methodDetailContent = methodDetail.selectFirst(".detail-content");

                    Element fieldList = methodDetailContent.selectFirst(".field-list");
                    if (fieldList != null) {
                        // parse list parameter
                        for (Element listElement : fieldList.children()) {
                            Parameter parameter = new Parameter(this);

                            if (parameter.parseDefault(listElement)) {
                                this.parameters.add(parameter);
                            }
                        }
                    } else {
                        // parse normal parameter
                        for (Element methodParam : methodDetailContent.children()) {
                            Parameter parameter = new Parameter(this);

                            if (parameter.parseDefault(methodParam)) {
                                this.parameters.add(parameter);
                            }
                        }
                    }
                    // Description for return value
                } else if (header.text().equals("Return value")) {
                    this.returnTypeDesc = methodDetail.selectFirst(".detail-content").text();
                }
            }
        }

        private void copyAndParseOverload(@NotNull Element paramList) {
            Method clonedMethod = new Method(this);

            for (Element param : paramList.children()) {
                Parameter parameter = new Parameter(clonedMethod);
                if (parameter.parseDefault(param)) {
                    clonedMethod.parameters.add(parameter);
                }
            }

            factorioType.methods.add(clonedMethod);
        }

        private void copyAndAdd() {
            Method clonedMethod = new Method(this);
            factorioType.methods.add(clonedMethod);
        }
    }

    /**
     * Represents a parameter of methods of factorio classes.
     * Also is used in some cases for variables.
     */
    public class Parameter {
        public String name;
        public String type;
        public String desc;
        public boolean optional = false;
        private Method method = null;

        public Parameter() {
        }

        public Parameter(Method method) {
            this.method = method;
        }

        /**
         * Copy-Constructor
         *
         * @param other copied Parameter
         */
        Parameter(@NotNull Parameter other) {
            this.name = other.name;
            this.type = other.type;
            this.desc = other.desc;
            this.optional = other.optional;
            this.method = other.method;
        }

        /**
         * Parses a parameter. This is also used to parse variables and table elements.
         * Example: https://lua-api.factorio.com/latest/LuaControlBehavior.html#LuaControlBehavior.get_circuit_network
         * <p>
         * returns false, when the param is somehow not correctly parse (no child elements, wrong layout, ...)
         *
         * @param methodParam The Element, that contains the parameter
         * @return Parameter parsed successfully
         */
        private boolean parseDefault(@NotNull Element methodParam) {
            // dont run on empty elements
            if (methodParam.childNodeSize() == 0) {
                return false;
            }

            // check if first element is not the param-name
            if (!methodParam.child(0).hasClass("param-name")) {
                Elements additionalParamLists = methodParam.select(".field-list");

                for (Element additionalParamList : additionalParamLists) {
                    // duplicate method as overload
                    if (method != null) {
                        method.copyAndParseOverload(additionalParamList);
                    }
                }
                return false;
            } else {
                // get name
                Element paramNameElement = methodParam.selectFirst(".param-name");

                // replace with valid param names
                this.name = paramNameElement.text().replace("-", "_");
                if (this.name.equals("function")) {
                    this.name = "func";
                } else if (this.name.equals("end")) {
                    this.name = "_end";
                }
                paramNameElement.remove();

                // get types
                Element paramType = methodParam.selectFirst(".param-type");
                if (paramType != null) {
                    this.type = parseType(paramType);
                    paramType.remove();
                }

                // get optional
                Element methodParamOpt = methodParam.selectFirst(".opt");
                if (methodParamOpt != null) {
                    this.optional = true;

                    // copy and add Method now (work further on the old Method)
                    if (method != null) {
                        method.copyAndAdd();
                    }

                    methodParamOpt.remove();
                }

                // old elements are removed, so everything else is the description
                this.desc = removeNewLines(methodParam.html().strip()).strip().replaceAll("^[:]+|[:]+$", "").strip();

                // parse literal types if string
                if (this.type != null && this.type.equals("string")) {
                    for (Element codeElement : methodParam.select("code")) {
                        this.type += "|'" + codeElement.text() + "'";
                    }
                }

                return true;
            }
        }
    }

    /**
     * The representation of a factorio class.
     * It saves all methods and attributes for the class. Also contains a string to represent parent classes.
     */
    public class FactorioType {
        public String name;
        public String parentType;
        public List<String> desc = new ArrayList<>();
        public List<Method> methods = new ArrayList<>();
        public List<Attribute> attributes = new ArrayList<>();

        /**
         * Parses the class from the document. It will also start the parsing of methods and attributes.
         * Some documents contain more than one class, so the typeId contains the css-id of the class to parse: https://lua-api.factorio.com/latest/LuaControlBehavior.html
         *
         * @param document The document with the factorioType in it
         * @param typeId   The css-id of the type to parse from the page
         */
        public void parseSingle(@NotNull Document document, String typeId) {
            Element typeBrief = document.selectFirst("[id=" + typeId + ".brief]");
            this.name = typeBrief.selectFirst(".type-name").text();

            if (document.selectFirst("h1").text().equals(this.name)) {
                Element briefDescription = document.selectFirst(".brief-description");
                if (briefDescription != null) {
                    this.desc.add(removeNewLines(briefDescription.html()));
                }
            }

            Elements listingLinks = typeBrief.select("a");
            for (Element listingLink : listingLinks) {
                if (listingLink.hasClass("sort")) {
                    break;
                }
                if (!listingLink.parent().hasClass("type-name")) {
                    this.parentType = listingLink.text();
                    break;
                }
            }

            Element typeData = document.selectFirst("#" + typeId);

            if (typeData.child(0).is("h2")) {
                Element elemContent = typeData.selectFirst(".element-content");
                for (Element curChild : elemContent.children()) {
                    if (!curChild.hasClass("element")) {
                        String curChildText = removeNewLines(curChild.html());
                        if (!curChildText.isEmpty()) {
                            this.desc.add(curChildText);
                        }
                    }
                }
            }

            Elements typeElements = typeData.select(".element");

            for (Element typeMethod : typeElements) {
                // Skip self element
                if (typeMethod.id().equals(typeId)) {
                    continue;
                }

                if (typeMethod.hasClass("element")) {
                    // Is element of this type
                    // check if element is attribute
                    Element attributeType = typeMethod.selectFirst(".attribute-type");
                    if (attributeType != null) {
                        // parse Attribute
                        Attribute attribute = new Attribute();
                        attribute.name = typeMethod.id();
                        attribute.parse(typeMethod);

                        this.attributes.add(attribute);
                    } else {
                        // parse Method
                        Method factorioTypeMethod = new Method(this);

                        // get name of element
                        Element methodNameElement = typeMethod.selectFirst(".element-name");
                        factorioTypeMethod.name = typeMethod.id();
                        factorioTypeMethod.parseSimple(methodNameElement, typeMethod);

                        // add method to
                        this.methods.add(factorioTypeMethod);
                    }
                } else {
                    // Is part of the description
                    this.desc.add(removeNewLines(typeMethod.html()));
                }
            }
        }

        /**
         * Save this FactorioType to a lua file.
         */
        private void saveToFile() {
            // create new file content
            StringBuilder typeFileContent = new StringBuilder();
            for (String s : this.desc) {
                typeFileContent.append("---").append(s).append(newLine);
            }
            typeFileContent.append("---@class ").append(this.name);
            if (this.parentType != null && !this.parentType.isEmpty()) {
                typeFileContent.append(" : ").append(this.parentType);
            }
            typeFileContent.append(newLine);
            typeFileContent.append("local ").append(this.name).append(" = {}").append(newLine).append(newLine);

            // add all methods
            for (Method method : this.methods) {
                StringBuilder methodPrint = new StringBuilder("function ");

                // add description
                for (String s : method.desc) {
                    typeFileContent.append("---").append(s).append(newLine);
                }
                methodPrint.append(method.name).append("(");
                boolean laterParam = false;

                // add parameters to output and to completeMethod
//                for (FactorioType.Parameter parameter : method.parameters) {
                for (int i = 0; i < method.parameters.size(); i++) {
                    Parameter parameter = method.parameters.get(i);
                    // Override Parameter, when type is null and add generic name
                    if (parameter.type == null || parameter.type.isEmpty()) {
                        parameter.type = parameter.name;
                        parameter.name = method.name.replace(".", "_") + "_Param_" + i;
                    }

                    typeFileContent.append("---@param ").append(parameter.name).append(" ").append(parameter.type);
                    if (parameter.desc != null && !parameter.desc.isEmpty()) {
                        typeFileContent.append(" ").append(parameter.desc);
                    }
                    typeFileContent.append(newLine);

                    if (laterParam) {
                        methodPrint.append(", ");
                    } else {
                        laterParam = true;
                    }
                    methodPrint.append(parameter.name);
                }
                methodPrint.append(") end");

                // add return value
                if (method.returnType != null && !method.returnType.isEmpty()) {
                    typeFileContent.append("---@return ").append(method.returnType);
                    if (method.returnTypeDesc != null && !method.returnTypeDesc.isEmpty()) {
                        typeFileContent.append(" ").append(method.returnTypeDesc);
                    }
                    typeFileContent.append(newLine);
                }

                // add complete method
                typeFileContent.append(methodPrint).append(newLine).append(newLine).append(newLine);
            }

            // add all attributes
            for (Attribute attribute : this.attributes) {
                // create new class as return type
                if (!attribute.returnParameters.isEmpty()) {
                    // create name of the class
                    String parameterClassName = attribute.name.replace(".", "_") + "_Result";

                    // define the new class
                    typeFileContent.append("---@class ").append(parameterClassName).append(newLine);
                    typeFileContent.append("local ").append(parameterClassName).append(" = {}").append(newLine);

                    // add the parameters to the class, so the structure is correct
                    for (Parameter returnParameter : attribute.returnParameters) {
                        if (returnParameter.desc != null && !returnParameter.desc.isEmpty()) {
                            typeFileContent.append("--- ").append(returnParameter.desc).append(newLine);
                        }
                        typeFileContent.append("---@type ").append(returnParameter.type).append(newLine);
                        typeFileContent.append(parameterClassName).append(".").append(returnParameter.name).append(" = nil").append(newLine);
                    }

                    // override type of the attribute
                    boolean attributeIsArray = attribute.type.endsWith("[]");
                    attribute.type = parameterClassName;
                    if (attributeIsArray) {
                        attribute.type += "[]";
                    }
                }

                // create description
                for (String s : attribute.desc) {
                    typeFileContent.append("--- ").append(s).append(newLine);
                }

                // Write read and write possibilities
                String rwText = "";
                if (attribute.read && !attribute.write) {
                    rwText += "Read-Only";
                } else if (!attribute.read && attribute.write) {
                    rwText += "Write-Only";
                } else if (attribute.read && attribute.write) {
                    rwText += "Read-Write";
                }
                if (!rwText.isEmpty()) {
                    typeFileContent.append("--- ").append(rwText).append(newLine);
                }

                // add type
                typeFileContent.append("---@type ").append(attribute.type).append(newLine);

                // add attribute definition
                typeFileContent.append(attribute.name).append(" = nil").append(newLine).append(newLine).append(newLine);
            }

            // create file
            String typeDir = saveDir + this.name + ".lua";
            saveStringToFile(typeDir, typeFileContent.toString());
        }
    }

    /**
     * Entry-point with creating the used directory to the save the directory to.
     * It will assure, that the directory is there and will start the download and parsing.
     */
    private void assureDir() {
        File dirFile = new File(saveDir);
        if (!dirFile.exists()) {
            // file does not exist ... create it
            if (dirFile.mkdirs()) {
                // download and parse API
                downloadAndParseAPI();
                return;
            }
        }
        Notification notification = notificationGroup.createNotification("Error creating the directories for the Factorio API.", NotificationType.ERROR);
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
     * Entry-point for the whole parsing.
     * Download the main-Page of the factorio-lua-api, parse all classes and start the other parsers.
     * Here also the indicator will be updated, to show the current percentage of the parsing.
     */
    private void downloadAndParseAPI() {
        String versionedApiLink = factorioApiBaseLink + config.selectedFactorioVersion.link;

        indicator.setIndeterminate(false);

        Document mainPage;
        try {
            mainPage = Jsoup.connect(versionedApiLink).get();
        } catch (IOException e) {
            System.out.println("error downloading the main API page");
            showDownloadingError(false);
            return;
        }

        String version = mainPage.selectFirst(".version").text();
        version = version.substring(9); // remove "Factorio " from version
        config.curVersion = version;

        // get links to full classes
        Element classes = mainPage.selectFirst(".brief-members");
        Elements classLinks = classes.select(".header a");

        // create List with all cleared links
        Set<String> links = new HashSet<>();
        for (Element classLink : classLinks) {
            links.add(classLink.attr("href").split("#")[0]);
        }

        // all classes + eventFilters + defines + concepts + globals
        maxTodo = links.size() + 1 + 3;

        updateIndicator();

        // parse EventFilters first
        parseEventFilters(versionedApiLink);

        updateIndicator();

        // Iterate over links
        for (String link : links) {
            Document singleDoc;
            String completeLink = versionedApiLink + link;

            try {
                singleDoc = Jsoup.connect(completeLink).get();
            } catch (Exception e) {
                System.out.println("couldnt load testDir");
//                e.printStackTrace();
                showDownloadingError(true);
                return;
            }

            // get the brief listings of all types on this page
            Elements briefListings = singleDoc.select(".brief-listing");
            for (Element briefListing : briefListings) {
                String listingId = briefListing.id();
                if (listingId != null && !listingId.isEmpty()) {
                    listingId = listingId.split("\\.")[0];

                    FactorioType factorioType = new FactorioType();
                    factorioType.parseSingle(singleDoc, listingId);
                    factorioType.saveToFile();
                }
            }

            updateIndicator();
        }

        // parse defines and events page
        parseDefines(versionedApiLink);

        updateIndicator();

        // parse concepts
        parseConcepts(versionedApiLink);

        updateIndicator();

        // parse global variables
        parseGlobals(mainPage);

        updateIndicator();
    }

    /**
     * Parses and saves the EventFilters page. This page got added in v0.17.75, earlier versions will not have this page and the download fails.
     * Event filters are custom Lua-Types, will be saved and shown as classes with elements.
     * Additionally an alias is created, from all EventFilters to <code>Filters</code>.
     * <p>
     * https://lua-api.factorio.com/latest/Event-Filters.html
     *
     * @param baseLink Link to the frontpage of the api with version
     */
    private void parseEventFilters(String baseLink) {
        Document eventFiltersPage;
        String eventFiltersLink = baseLink + "Event-Filters.html";

        try {
            eventFiltersPage = Jsoup.connect(eventFiltersLink).get();
        } catch (IOException e) {
            System.out.println("error downloading the Event-Filters page, old versions dont have them.");
            return;
        }

        class EventFilter {
            private String name;
            private List<Parameter> parameters = new ArrayList<>();
        }
        List<EventFilter> eventFilters = new ArrayList<>();
        Elements elements = eventFiltersPage.select(".element");
        for (Element element : elements) {
            EventFilter eventFilter = new EventFilter();
            eventFilter.name = element.id();

            // Iterate over all lists
            Elements lists = element.select(".element-content > ul");
            for (Element list : lists) {
                // Iterate over all children in this list
                for (Element fieldListChild : list.children()) {
                    if (list.hasClass("field-list")) {
                        // Run things differently on field-list object. It contains the default params
                        Parameter parameter = new Parameter();
                        parameter.parseDefault(fieldListChild);
                        eventFilter.parameters.add(parameter);
                    } else {
                        // Rest is description with literal types
                        Element code = fieldListChild.selectFirst("code");
                        eventFilter.parameters.get(0).type += "|'" + code.text() + "'";
                        Element subFieldList = fieldListChild.selectFirst(".field-list");

                        // Some literal types are in need of another param
                        if (subFieldList != null) {
                            for (Element subFieldListChild : subFieldList.children()) {
                                Parameter parameter = new Parameter();
                                parameter.parseDefault(subFieldListChild);
                                eventFilter.parameters.add(parameter);
                            }
                        }
                    }
                }
            }
            eventFilters.add(eventFilter);
        }

        // Save the filters to a file
        StringBuilder aliasEventFilter = new StringBuilder();
        StringBuilder eventFilterContent = new StringBuilder();
        for (EventFilter eventFilter : eventFilters) {
            eventFilterContent.append("---@class ").append(eventFilter.name).append(newLine);
            eventFilterContent.append("local ").append(eventFilter.name).append(" = {}").append(newLine).append(newLine);

            if (aliasEventFilter.length() > 0) {
                aliasEventFilter.append("|");
            }
            aliasEventFilter.append(eventFilter.name).append("[]");

            for (Parameter parameter : eventFilter.parameters) {
                if (parameter.desc != null && !parameter.desc.isEmpty()) {
                    eventFilterContent.append("--- ").append(parameter.desc).append(newLine);
                }
                eventFilterContent.append("---@type ").append(parameter.type).append(newLine);
                eventFilterContent.append(eventFilter.name).append(".").append(parameter.name).append(" = nil").append(newLine).append(newLine);
            }
            eventFilterContent.append(newLine);
        }
        eventFilterContent.append("---@alias Filters ").append(aliasEventFilter);

        String eventFiltersFile = saveDir + "EventFilters.lua";
        saveStringToFile(eventFiltersFile, eventFilterContent.toString());
    }

    /**
     * Parses all types inside the given element. It will look inside the element and tries to find more `.param-type` elements as their children
     * Will also parse functions and tables. They then look a little different:
     * Tables: <code>table<Type, Type>`</code>
     * Functions: <code>fun(name, name, ...)</code>
     * Arrays will also be parsed, instead of the text `array of`, `[]` will be added to the type.
     *
     * @param typeElement The parent param-type. It is around all param-types
     * @return list with all types
     */
    private String parseType(@NotNull Element typeElement) {
        String typeText = typeElement.text();
        if (typeText.startsWith("array of")) {
            return typeElement.select(".param-type").last().text() + "[]";
        } else if (typeText.startsWith("function")) {
            StringBuilder functionBuilder = new StringBuilder("fun(");
            boolean first = true;
            for (Element typeChild : typeElement.children()) {
                if (typeChild.hasClass("param-type")) {
                    if (!first) {
                        functionBuilder.append(", ");
                    } else {
                        first = false;
                    }
                    String typeChildText = typeChild.text();
                    functionBuilder.append(StringUtil.decapitalize(typeChildText)).append(":").append(typeChildText);
                }
            }
            functionBuilder.append(")");
            return functionBuilder.toString();
        } else if (typeText.startsWith("CustomDictionary") || typeText.startsWith("dictionary")) {
            StringBuilder dictBuilder = new StringBuilder("table<");
            boolean first = true;
            for (Element typeChild : typeElement.children()) {
                if (typeChild.hasClass("param-type")) {
                    if (!first) {
                        dictBuilder.append(", ");
                    } else {
                        first = false;
                    }
                    dictBuilder.append(parseType(typeChild));
                }
            }
            dictBuilder.append(">");
            return dictBuilder.toString();
        } else {
            StringBuilder generalBuilder = new StringBuilder();
            boolean first = true;
            for (Element typeChild : typeElement.children()) {
                if (typeChild.hasClass("param-type")) {
                    if (!first) {
                        generalBuilder.append("|");
                    } else {
                        first = false;
                    }
                    generalBuilder.append(parseType(typeChild));
                }
            }

            // when no children parsed
            if (first) {
                return typeText;
            } else {
                return generalBuilder.toString();
            }
        }
    }

    /**
     * parses and saves the defines page of the factorio API.
     * The result is a multistructure with all defines and in a table.
     * The events table is parsed form the events page: https://lua-api.factorio.com/latest/events.html
     * Defines page: https://lua-api.factorio.com/latest/defines.html
     *
     * @param baseLink The Link to the frontpage of this api version
     */
    private void parseDefines(String baseLink) {
        Document definesPage;
        String definesLink = baseLink + "defines.html";

        try {
            definesPage = Jsoup.connect(definesLink).get();
        } catch (IOException e) {
            System.out.println("error downloading the defines page");
//            e.printStackTrace();
            showDownloadingError(true);
            return;
        }

        List<String> defineClasses = new ArrayList<>();
        List<Pair<String, String>> defineValues = new ArrayList<>();
        Elements allDefines = definesPage.select(".element");
        for (Element define : allDefines) {
            String valueId = define.id();
            if (valueId.contains("events")) {
                continue;
            }
            if (define.is("tr")) {
                // is value
                String desc = define.selectFirst(".description").html();
                defineValues.add(Pair.create(valueId, desc));
            } else {
                // is class
                defineClasses.add(valueId);
            }
        }


        // parse event page here too
        Document eventsPage;
        String eventsLink = baseLink + "events.html";

        try {
            eventsPage = Jsoup.connect(eventsLink).get();
        } catch (IOException e) {
            System.out.println("error downloading the events page");
//            e.printStackTrace();
            showDownloadingError(true);
            return;
        }

        for (Element event : eventsPage.select(".element")) {
            if (event.id().equals("All events")) {
                continue;
            }

            String eventName = event.id();
            if (!eventName.isEmpty()) {
                eventName = "defines.events." + eventName;
                String desc = removeNewLines(event.selectFirst(".element-content").html());
                defineValues.add(Pair.create(eventName, desc));
            }
        }
        defineClasses.add("defines.events");


        // save defines to file
        StringBuilder definesFileContent = new StringBuilder();
        definesFileContent.append("---@class defines").append(newLine);
        definesFileContent.append("local defines = {}").append(newLine).append(newLine).append(newLine);

        for (String defineClass : defineClasses) {
            definesFileContent.append("---@class ").append(defineClass).append(newLine);
            definesFileContent.append("local ").append(defineClass).append(" = {}").append(newLine).append(newLine).append(newLine);
        }

        for (Pair<String, String> defineValue : defineValues) {
            String desc = defineValue.second;
            if (!desc.isEmpty()) {
                definesFileContent.append("--- ").append(desc).append(newLine);
            }
            definesFileContent.append("---@type nil").append(newLine);
            definesFileContent.append(defineValue.first).append(" = nil").append(newLine).append(newLine).append(newLine);
        }

        String typeFile = saveDir + "defines.lua";
        saveStringToFile(typeFile, definesFileContent.toString());
    }

    private void parseConcepts(String baseLink) {
        Document conceptsPage;
        String conceptsLink = baseLink + "Concepts.html";

        try {
            conceptsPage = Jsoup.connect(conceptsLink).get();
        } catch (IOException e) {
            System.out.println("error downloading the defines page");
//            e.printStackTrace();
            showDownloadingError(true);
            return;
        }

        // generate factorio types first
        Elements briefListings = conceptsPage.select(".brief-listing");
        for (int i = 1; i < briefListings.size(); i++) {
            Element briefListing = briefListings.get(i);
            String listingId = briefListing.id().split("\\.")[0];
            FactorioType factorioType = new FactorioType();
            factorioType.parseSingle(conceptsPage, listingId);
            factorioType.saveToFile();

            // remove factorioType from dom
            conceptsPage.selectFirst("#" + listingId).remove();
        }

        // generate general concept classes
        class ConceptClass {
            private String name;
            private List<String> desc = new ArrayList<>();
            private List<Parameter> parameters = new ArrayList<>();
        }
        List<ConceptClass> conceptClasses = new ArrayList<>();
        Elements elements = conceptsPage.select(".element");
        for (Element element : elements) {
            ConceptClass conceptClass = new ConceptClass();
            conceptClass.name = element.id();

            for (Element contentChild : element.selectFirst(".element-content").children()) {
                if (contentChild.hasClass("field-list")) {
                    for (Element child : contentChild.children()) {
                        Parameter parameter = new Parameter();

                        parameter.parseDefault(child);
                        conceptClass.parameters.add(parameter);
                    }
                } else {
                    String contentChildHtml = removeNewLines(contentChild.html()).strip();
                    if (!contentChildHtml.isEmpty()) {
                        conceptClass.desc.add(contentChildHtml);
                    }
                }
            }
            conceptClasses.add(conceptClass);
        }

        // save to file
        StringBuilder conceptsFileContent = new StringBuilder();

        for (ConceptClass conceptClass : conceptClasses) {
            for (String s : conceptClass.desc) {
                conceptsFileContent.append("--- ").append(s).append(newLine);
            }

            conceptsFileContent.append("---@class ").append(conceptClass.name).append(newLine);
            conceptsFileContent.append("local ").append(conceptClass.name).append(" = {}").append(newLine).append(newLine);

            for (Parameter parameter : conceptClass.parameters) {
                if (parameter.desc != null && !parameter.desc.isEmpty()) {
                    conceptsFileContent.append("--- ").append(parameter.desc).append(newLine);
                }
                conceptsFileContent.append("---@type ").append(parameter.type).append(newLine);
                conceptsFileContent.append(conceptClass.name).append(".").append(parameter.name).append(" = nil").append(newLine).append(newLine);
            }
            conceptsFileContent.append(newLine);
        }

        String conceptsFile = saveDir + "concepts.lua";
        saveStringToFile(conceptsFile, conceptsFileContent.toString());
    }

    /**
     * Parses and saves the global variables of the factorio API
     * https://lua-api.factorio.com/latest/ > Overview > Global classes
     *
     * @param mainPage The jsoup page to parse the globals from
     */
    private void parseGlobals(@NotNull Document mainPage) {
        StringBuilder globalsFileContent = new StringBuilder();
        Element fieldList = mainPage.selectFirst(".field-list");
        for (Element fieldListChild : fieldList.children()) {
            Parameter parameter = new Parameter();
            parameter.parseDefault(fieldListChild);

            if (parameter.desc != null && !parameter.desc.isEmpty()) {
                globalsFileContent.append("--- ").append(parameter.desc).append(newLine);
            }
            globalsFileContent.append("---@type ").append(parameter.type).append(newLine);
            globalsFileContent.append(parameter.name).append(" = {}").append(newLine).append(newLine);
        }

        String globalsFile = saveDir + "globals.lua";
        saveStringToFile(globalsFile, globalsFileContent.toString());
    }
}

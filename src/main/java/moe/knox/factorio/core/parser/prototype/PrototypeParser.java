package moe.knox.factorio.core.parser.prototype;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.core.version.FactorioApiVersion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrototypeParser {
    private static final Logger LOG = Logger.getInstance(PrototypeParser.class);
    private final static String prototypesBaseLink = "https://wiki.factorio.com";
    private final static String NEW_LINE = System.lineSeparator();
    private final static List<String> rootTypes = new ArrayList<>() {{
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
    private final static List<String> prototypeTypeWhitelist = new ArrayList<>() {{
        add("Types/ItemProductPrototype");
        add("Types/ItemToPlace");
        add("Types/DamagePrototype");
    }};

    /**
     * map of all propertyTypes `typeName` > `link`
     */
    private Map<String, String> propertyTypes = new HashMap<>();

    private final Path prototypesRootPath;

    public PrototypeParser(Path prototypesRootPath) {

        this.prototypesRootPath = prototypesRootPath;
    }

    public @Nullable Path getPrototypePath(FactorioApiVersion version)
    {
        Path versionPath = prototypesRootPath.resolve(version.version());

        return Files.exists(versionPath) ? versionPath : null;
    }

    public void removeFiles() {
        FileUtil.delete(prototypesRootPath.toFile());
    }

    public void parse(FactorioApiVersion selectedVersion) throws IOException {
        Path prototypesSubdirPath = prototypesRootPath.resolve(selectedVersion.version());

        if (Files.exists(prototypesSubdirPath)) {
            return;
        }

        Files.createDirectories(prototypesSubdirPath);

        parseInternal(selectedVersion);
    }

    public List<String> parsePrototypeTypes() throws IOException {
        String prototypesOverviewLink = prototypesBaseLink + "/Prototype_definitions";
        Document protoOverview = Jsoup.connect(prototypesOverviewLink).get();
        Elements prototypeElements = protoOverview.select("#mw-content-text ul a");
        List<String> prototypeIds = new ArrayList<>();
        Path tempPrototypesDir = Files.createTempDirectory("prototypes");

        for (Element prototypeElement : prototypeElements) {
            Prototype prototype = new Prototype(tempPrototypesDir, true);
            if (!prototype.parsePrototype(prototypeElement.attr("href"))) {
                break;
            }
            if (prototype.id != null && !prototype.id.equals("abstract")) {
                prototypeIds.add(prototype.id);
            }
        }

        return prototypeIds;
    }

    private void parseInternal(FactorioApiVersion selectedVersion) throws IOException {
        Path prototypesSubdirPath = prototypesRootPath.resolve(selectedVersion.version());

        parsePrototypeTypes();

        propertyTypes.forEach((typeName, typeLink) -> {
            if (!rootTypes.contains(typeName)) {
                // parse additional types, like normal Prototypes
                Prototype prototype = new Prototype(prototypesSubdirPath);
                prototype.name = typeName;

                // All unsuccessful types are hardcoded in the lua library
                prototype.parsePrototype(typeLink);
            }
        });
    }

    private class Prototype {
        String name;
        String id;
        String parentType;
        boolean catchTypes;
        List<String> description = new ArrayList<>();
        List<Property> properties = new ArrayList<>();
        private Path prototypeDir;

        public Prototype(Path prototypeDir, boolean catchTypes) {
            this.prototypeDir = prototypeDir;
            this.catchTypes = catchTypes;
        }

        public Prototype(Path prototypeDir) {
            this.prototypeDir = prototypeDir;
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
                if (e instanceof HttpStatusException && ((HttpStatusException) e).getStatusCode() == 404) {
                    return true;
                }

                LOG.error("error downloading the single prototype page: %s".formatted(link));
                LOG.error(e);

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
                            if (!Jsoup.clean(splittedType[0], Safelist.none()).equals("Type")) {
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

        /**
         * removed "::" and all variants of newLines from the given string and returns it.
         *
         * @param s remove from this String
         * @return string with removed things
         */
        @NotNull
        @Contract(pure = true)
        private String removeNewLines(@NotNull String s) {
            return s.replaceAll("(::)|(\\r\\n|\\r|\\n)", "");
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
            String typeDir = prototypeDir.resolve(this.name + ".lua").toString();
            saveStringToFile(typeDir, typeFileContent.toString());
        }

        private void saveStringToFile(String filePath, String fileContent) {
            try {
                File file = new File(filePath);
                file.createNewFile();
                com.google.common.io.Files.write(fileContent.getBytes(), file);
            } catch (IOException e) {
                LOG.error(e);
            }
        }

        private void saveTable(StringBuilder fileContent, List<String> description, String name, @Nullable String parentType, List<Property> properties) {
            for (String s : description) {
                fileContent.append("---").append(s).append(NEW_LINE);
            }

            fileContent.append("---@class ").append(name);
            if (parentType != null && !parentType.isEmpty()) {
                fileContent.append(" : ").append(parentType);
            }
            fileContent.append(NEW_LINE);
            fileContent.append("local ").append(name).append(" = {}").append(NEW_LINE).append(NEW_LINE);

            // add all properties
            for (Property property : properties) {
                for (String s : property.description) {
                    fileContent.append("--- ").append(s).append(NEW_LINE);
                }

                // add type
                fileContent.append("---@type ").append(property.type).append(NEW_LINE);

                // add attribute definition
                fileContent.append(name).append(".").append(property.name).append(" = nil").append(NEW_LINE).append(NEW_LINE);

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

package moe.knox.factorio.parser;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.FactorioAutocompletionConfig;
import moe.knox.factorio.FactorioAutocompletionState;
import moe.knox.factorio.library.FactorioLibraryProvider;
import moe.knox.factorio.parser.apiData.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioApiParser extends FactorioParser {
    public static String apiRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_api/";
    public static String factorioApiBaseLink = "https://lua-api.factorio.com/";

    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;

    public FactorioApiParser(@Nullable Project project, String saveDir, @NlsContexts.ProgressTitle @NotNull String title, boolean canBeCancelled) {
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
    synchronized public static String getCurrentApiLink(Project project) {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = apiRootPath + config.selectedFactorioVersion.link;

        // check if API is downloaded
        File apiPathFile = new File(apiPath);
        if (apiPathFile.exists()) {
            return apiPath;
        } else {
            // request download API
            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioApiParser(project, apiPath, "Download and Parse Factorio API", false));
            }
            return null;
        }
    }

    public static void removeCurrentAPI(Project project) {
        if (!downloadInProgress.get()) {
            FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
            String apiPath = apiRootPath + config.selectedFactorioVersion.link;
            FileUtil.delete(new File(apiPath));
            FactorioLibraryProvider.reload();
        }
    }

    public static void checkForUpdate(Project project) {
        FactorioAutocompletionState config = FactorioAutocompletionState.getInstance(project);
        String apiPath = apiRootPath + config.selectedFactorioVersion.link;

        if (config.selectedFactorioVersion.desc.equals("Latest version")) {
            Document doc = null;
            try {
                doc = Jsoup.connect("https://lua-api.factorio.com/").get();
            } catch (IOException e) {
//                    e.printStackTrace();
                Notification notification = getNotificationGroup().createNotification("Error checking new Version. Manual update in the Settings.", NotificationType.WARNING);
                notification.addAction(new NotificationAction("Open Settings") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, FactorioAutocompletionConfig.class);
                    }
                });
            }
            if (!doc.select("a").get(1).text().equals(config.curVersion)) {
                // new version detected, update it
                removeCurrentAPI(project);
                if (downloadInProgress.compareAndSet(false, true)) {
                    ProgressManager.getInstance().run(new FactorioApiParser(project, apiPath, "Download and Parse Factorio API", false));
                }
            }
        }
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
            } else {
                Notification notification = getNotificationGroup().createNotification("Error creating the directories for the Factorio API.", NotificationType.ERROR);
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
     * Entry-point for the whole parsing.
     * Download the main-Page of the factorio-lua-api, parse all classes and start the other parsers.
     * Here also the indicator will be updated, to show the current percentage of the parsing.
     */
    private void downloadAndParseAPI() {
        String versionedApiLink = factorioApiBaseLink + config.selectedFactorioVersion.link + "runtime-api.json";

        JsonAPI jsonAPI;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new URL(versionedApiLink).openStream());
            jsonAPI = JsonAPI.read(inputStreamReader);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        config.curVersion = jsonAPI.application_version;

        String saveFile = saveDir + "factorio.lua";
        // create file

        OutputStreamWriter output;
        try {
            File file = new File(saveFile);
            file.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(file, false);
            output = new OutputStreamWriter(outputStream);
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }

        try {
            // builtin types done in `resources/library/builtin-types.lua`

            writeGlobalsObjects(output, jsonAPI.globalObjects);

            output.append("---@class defines").append(newLine);
            output.append("defines = {}").append(newLine).append(newLine);
            writeDefines(output, jsonAPI.defines, "defines");

            // TODO: implement autocompletion for events

            writeClasses(output, jsonAPI.classes);

            writeConcepts(output, jsonAPI.concepts);

            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }
    }

    void writeEmptyLine(OutputStreamWriter output) throws IOException {
        output.append("---").append(newLine);
    }

    void writeDescLine(OutputStreamWriter output, List<String> lines) throws IOException {
        if (lines != null && !lines.isEmpty()) {
            for (String line : lines) {
                writeEmptyLine(output);
                writeDescLine(output, line);
            }
        }
    }

    void writeDescLine(OutputStreamWriter output, String line) throws IOException {
        if (!line.isEmpty()) {
            line = line.replace('\n', ' ');
            output.append("--- ").append(line).append(newLine);
        }
    }

    void writeReadWrite(OutputStreamWriter output, boolean read, boolean write) throws IOException {
        if (read && write) {
            output.append("--- ").append("Read-Write").append(newLine);
        } else if (read) {
            output.append("--- ").append("Read-Only").append(newLine);
        } else if (write) {
            output.append("--- ").append("Write-Only").append(newLine);
        }
    }

    void writeSee(OutputStreamWriter output, List<String> seeAlso) throws IOException {
        if (seeAlso != null && !seeAlso.isEmpty()) {
            for (String see : seeAlso) {
                see = see.replace("::", "#");
                output.append("---@see ").append(see).append(newLine);
            }
        }
    }

    void writeClass(OutputStreamWriter output, String className) throws IOException {
        writeClass(output, className, "");
    }

    void writeClass(OutputStreamWriter output, String className, String parentClass) throws IOException {
        output.append("---@class ").append(className);
        if (!parentClass.isEmpty()) {
            output.append(" : ").append(parentClass);
        }
        output.append(newLine);
    }

    void writeClass(OutputStreamWriter output, String className, List<String> parentClasses) throws IOException {
        if (parentClasses != null && !parentClasses.isEmpty()) {
            writeClass(output, className, parentClasses.get(0));
        } else {
            writeClass(output, className);
        }
    }

    void writeShape(OutputStreamWriter output, String name) throws IOException {
        output.append("---@shape ").append(name).append(newLine);
    }

    void writeField(OutputStreamWriter output, String name, Type type, String description) throws IOException {
        writeField(output, name, type, description, false);
    }

    void writeField(OutputStreamWriter output, String name, String type, String description) throws IOException {
        writeField(output, name, type, description, false);
    }

    void writeField(OutputStreamWriter output, String name, Type type, String description, boolean withNil) throws IOException {
        writeField(output, name, getType(type), description, withNil);
    }

    void writeField(OutputStreamWriter output, String name, String type, String description, boolean withNil) throws IOException {
        output.append("---@field ").append(name).append(' ').append(type);

        if (withNil) {
            output.append("|nil");
        }

        output.append(' ').append(description).append(newLine);
    }

    void writeType(OutputStreamWriter output, String type) throws IOException {
        writeType(output, type, false);
    }

    void writeType(OutputStreamWriter output, Type type) throws IOException {
        writeType(output, getType(type), false);
    }

    void writeType(OutputStreamWriter output, Type type, boolean optional) throws IOException {
        writeType(output, getType(type), optional);
    }

    void writeType(OutputStreamWriter output, String type, boolean optional) throws IOException {
        output.append("---@type ").append(type);
        if (optional) {
            output.append("|nil");
        }
        output.append(newLine);
    }

    void writeParam(OutputStreamWriter output, String name, Type type, String description) throws IOException {
        writeParam(output, name, getType(type), description);
    }

    void writeParam(OutputStreamWriter output, String name, String type) throws IOException {
        writeParam(output, name, type, "");
    }

    void writeParam(OutputStreamWriter output, String name, String type, String description) throws IOException {
        description = description.replace('\n', ' ');
        output.append("---@param ").append(name).append(' ').append(type).append(' ').append(description).append(newLine);
    }

    void writeReturn(OutputStreamWriter output, Type type, String desc) throws IOException {
        output.append("---@return ").append(getType(type)).append(' ');
        if (!desc.isEmpty()) {
            desc = desc.replace('\n', ' ');
            output.append(desc);
        }
        output.append(newLine);
    }

    void writeOverload(OutputStreamWriter output, List<Parameter> parameters, String stopAt) throws IOException {
        writeOverload(output, parameters, null, stopAt);
    }

    void writeOverload(OutputStreamWriter output, List<Parameter> parameters, Type returnType) throws IOException {
        writeOverload(output, parameters, returnType, null);
    }

    void writeOverload(OutputStreamWriter output, List<Parameter> parameters, Type returnType, String stopAt) throws IOException {
        // ---@overload fun(param1:A,param2:B):R

        output.append("---@overload fun(");

        boolean first = true;
        for (Parameter parameter : parameters) {
            if (stopAt != null && stopAt == parameter.name) {
                break;
            }

            if (first) {
                first = false;
            } else {
                output.append(',');
            }

            output.append(parameter.name).append(':').append(getType(parameter.type));
        }

        output.append(')');

        if (returnType != null) {
            output.append(':').append(getType(returnType));
        }

        output.append(newLine);
    }

    void writeAliasStringLiteral(OutputStreamWriter output, String name, List<String> types) throws IOException {
        output.append("---@alias ").append(name);

        boolean first = true;
        for (String type : types) {
            if (first) {
                first = false;
            } else {
                output.append('|');
            }

            output.append('"').append(type).append('"');
        }
        output.append(newLine);
    }

    void writeAlias(OutputStreamWriter output, String name, List<Type> types) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (Type type : types) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append('|');
            }
            stringBuilder.append(getType(type));
        }
        writeAlias(output, name, stringBuilder.toString());
    }

    void writeAlias(OutputStreamWriter output, String name, String type) throws IOException {
        output.append("---@alias ").append(name).append(' ').append(type).append(newLine);
    }

    String getType(Type type) {
        if (type.isSimpleString) {
            return type.type;
        }

        Type.ComplexData data = type.data;
        switch (data.complexType) {
            case "variant": {
                StringBuilder stringBuilder = new StringBuilder();
                boolean first = true;
                for (Type option : data.variant.options) {
                    if (!first) {
                        stringBuilder.append('|');
                    }
                    first = false;
                    stringBuilder.append(getType(option));
                }

                return stringBuilder.toString();
            }
            case "array": {
                StringBuilder stringBuilder = new StringBuilder();
                // A[]
                try {
                    stringBuilder.append(getType(data.array.value)).append("[]");
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                return stringBuilder.toString();
            }
            case "dictionary":
            case "LuaCustomTable": {
                StringBuilder stringBuilder = new StringBuilder();
                // table<A, B>
                stringBuilder.append("table<").append(getType(data.dictionary.key)).append(", ").append(getType(data.dictionary.value)).append(">");
                return stringBuilder.toString();
            }
            case "function": {
                StringBuilder stringBuilder = new StringBuilder();
                // fun(param:A, param2:B):RETURN_TYPE
                stringBuilder.append("fun(");
                int i = 0;
                for (Type parameter : data.function.parameters) {
                    if (i > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append("param").append(i).append(':').append(getType(parameter));
                    ++i;
                }
                stringBuilder.append(")");
            }
            case "LuaLazyLoadedValue": {
                return "LuaLazyLoadedValue";
                // TODO override `LuaLazyLoadedValue` class with generic
            }
            case "table": {
                return getAnonymousTableType(data.table.parameters);
            }
            default:
                throw new IllegalStateException("Unexpected value: " + data);
        }
    }

    String getAnonymousTableType(List<Parameter> parameters) {
        // {["huhu"]:number, ["baum"]:string}
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('{');
        boolean first = true;
        for (Parameter parameter : parameters) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(",");
            }
            stringBuilder.append("[\"").append(parameter.name).append("\"]:").append(getType(parameter.type));
            if (parameter.optional) {
                stringBuilder.append("|nil");
            }
        }
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    void writeObjDef(OutputStreamWriter output, String className) throws IOException {
        writeObjDef(output, className, false);
    }

    void writeObjDef(OutputStreamWriter output, String className, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        output.append(className).append(" = {}").append(newLine);
    }

    void writeValDef(OutputStreamWriter output, String name) throws IOException {
        writeValDef(output, name, null, false);
    }

    void writeValDef(OutputStreamWriter output, String name, String parent) throws IOException {
        writeValDef(output, name, parent, false);
    }

    void writeValDef(OutputStreamWriter output, String name, boolean local) throws IOException {
        writeValDef(output, name, null, local);
    }

    void writeValDef(OutputStreamWriter output, String name, String parent, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        if (parent != null && !parent.isEmpty()) {
            if (name.contains("-")) {
                output.append(parent).append("[\"").append(name).append("\"]");
            } else {
                output.append(parent).append('.').append(name);
            }
        } else {
            output.append(name).append(" = nil").append(newLine);
        }

        output.append(" = nil").append(newLine);
    }

    void writeFunctionDef(OutputStreamWriter output, String className, String functionName, String... params) throws IOException {
        output.append("function ").append(className).append('.').append(functionName).append('(');
        boolean first = true;
        for (String param : params) {
            if (first) {
                first = false;
            } else {
                output.append(", ");
            }
            output.append(param);
        }
        output.append(") end").append(newLine);
    }

    void writeGlobalsObjects(OutputStreamWriter output, List<GlobalObject> globalObjects) throws IOException {
        // global objects
        for (GlobalObject globalObject : globalObjects) {
            writeDescLine(output, globalObject.description);
            writeType(output, globalObject.type);
            writeObjDef(output, globalObject.name);
            output.append(newLine);
        }
        output.append(newLine);
    }

    void writeDefines(OutputStreamWriter output, List<Define> defines, String parents) throws IOException {
        for (Define define : defines) {
            writeDescLine(output, define.description);

            StringWriter subDefine = new StringWriter();
            subDefine.append(parents).append('.').append(define.name);
            writeClass(output, subDefine.toString());
            writeObjDef(output, subDefine.toString());
            output.append(newLine);

            if (define.subkeys != null && !define.subkeys.isEmpty()) {
                writeDefines(output, define.subkeys, subDefine.toString());
            }
            if (define.values != null && !define.values.isEmpty()) {
                writeDefineValues(output, define.values, subDefine.toString());
            }
        }
        output.append(newLine);
    }

    void writeDefineValues(OutputStreamWriter output, List<BasicMember> defines, String parents) throws IOException {
        for (BasicMember define : defines) {
            writeDescLine(output, define.description);
            writeType(output, "nil");
            writeValDef(output, define.name, parents);
            output.append(newLine);
        }
    }

    void writeClasses(OutputStreamWriter output, List<FactorioClass> classes) throws IOException {
        for (FactorioClass factorioClass : classes) {
            writeDescLine(output, factorioClass.description);
            writeDescLine(output, factorioClass.notes);
            writeDescLine(output, factorioClass.examples);
            writeSee(output, factorioClass.seeAlso);

            for (Operator operator : factorioClass.operators) {
                if (operator.name == "call") {
                    writeOverload(output, operator.method.parameters, operator.method.returnType);
                }
            }

            writeClass(output, factorioClass.name, factorioClass.baseClasses);
            writeObjDef(output, factorioClass.name, true);
            output.append(newLine);

            writeAttributes(output, factorioClass.attributes, factorioClass.name);
            writeMethods(output, factorioClass.methods, factorioClass.name);

            output.append(newLine);
        }
    }

    void writeAttributes(OutputStreamWriter output, List<Attribute> attributes, String className) throws IOException {
        for (Attribute attribute : attributes) {
            writeDescLine(output, attribute.description);
            writeDescLine(output, attribute.notes);
            writeDescLine(output, attribute.examples);
            writeSee(output, attribute.seeAlso);
            writeReadWrite(output, attribute.read, attribute.write);
            writeType(output, attribute.type);
            writeValDef(output, attribute.name, className);
            output.append(newLine);
        }
    }

    void writeMethods(OutputStreamWriter output, List<Method> methods, String className) throws IOException {
        for (Method method : methods) {
            writeDescLine(output, method.description);
            writeDescLine(output, method.notes);
            writeDescLine(output, method.examples);
            writeSee(output, method.seeAlso);

            if (method.takesTable) {
                // This is a table function (use anonymous function as only param)
                String paramType = getAnonymousTableType(method.parameters);

                writeParam(output, "param", paramType);

                if (method.returnType != null) {
                    writeReturn(output, method.returnType, method.returnDescription);
                }

                writeFunctionDef(output, className, method.name, "param");
            } else {
                List<String> strList = new ArrayList<>();

                for (Parameter parameter : method.parameters) {
                    writeParam(output, parameter.name, parameter.type, parameter.description);

                    if (parameter.optional) {
                        writeOverload(output, method.parameters, method.returnType, parameter.name);
                    }

                    strList.add(parameter.name);
                }

                if (method.returnType != null) {
                    writeReturn(output, method.returnType, method.returnDescription);
                }

                writeFunctionDef(output, className, method.name, strList.toArray(new String[0]));
            }
            output.append(newLine);
        }
    }

    void writeConcepts(OutputStreamWriter output, List<Concept> concepts) throws IOException {
        for (Concept concept : concepts) {
            switch (concept.category) {
                case "table": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeClass(output, concept.name);
                    writeObjDef(output, concept.name, true);
                    output.append(newLine);

                    for (Parameter parameter : concept.table.parameters) {
                        writeDescLine(output, parameter.description);
                        writeType(output, parameter.type, parameter.optional);
                        writeValDef(output, parameter.name, concept.name);
                        output.append(newLine);
                    }
                    break;
                }
                case "table_or_array": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeShape(output, concept.name);

                    int i = 1;
                    for (Parameter parameter : concept.tableOrArray.parameters) {
                        writeField(output, parameter.name, parameter.type, parameter.description);
                        writeField(output, "[" + i + "]", parameter.type, parameter.description);
                        ++i;
                    }

                    output.append(newLine);
                    break;
                }
                case "enum": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeClass(output, concept.name);

                    for (BasicMember option : concept._enum.options) {
                        String desc = "(Enum) " + option.description;
                        writeField(output, option.name, "number", desc);
                    }

                    output.append(newLine);
                    break;
                }
                case "flag": {
                    // define string-literal type
                    String aliasName = concept.name + "Value";
                    List<String> types = new ArrayList<>();
                    for (BasicMember option : concept.flag.options) {
                        types.add(option.name);
                    }
                    writeAliasStringLiteral(output, aliasName, types);
                    output.append(newLine);

                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);

                    // actual type is string-literal array
                    writeAlias(output, concept.name, aliasName + "[]");

                    output.append(newLine);
                    break;
                }
                case "union": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);

                    List<Type> types = new ArrayList<>();
                    for (Concept.CategoryUnion.Spec option : concept.union.options) {
                        types.add(option.type);
                        writeDescLine(output,getType(option.type) + ": " + option.description);
                    }
                    writeAlias(output, concept.name, types);
                    break;
                }
                case "filter": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeShape(output, concept.name);
                    writeObjDef(output, concept.name, true);

                    for (Parameter parameter : concept.filter.parameters) {
                        writeDescLine(output, parameter.description);
                        writeType(output, parameter.type, parameter.optional);
                        writeValDef(output, parameter.name, concept.name);
                        output.append(newLine);
                    }
                    break;
                }
                case "struct": {
                    writeDescLine(output, concept.description);
                    writeDescLine(output, concept.notes);
                    writeDescLine(output, concept.examples);
                    writeSee(output, concept.seeAlso);
                    writeClass(output, concept.name);
                    writeObjDef(output, concept.name, true);

                    writeAttributes(output, concept.struct.attributes, concept.name);
                    output.append(newLine);
                    break;
                }
            }
        }
    }
}

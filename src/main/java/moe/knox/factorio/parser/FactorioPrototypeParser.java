package moe.knox.factorio.parser;

import com.google.gson.Gson;
import com.intellij.notification.*;
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
import moe.knox.factorio.parser.prototypeData.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioPrototypeParser extends FactorioParser {
    protected static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup("Factorio Prototype Download");
    }

    public static final String prototypeRootPath = PathManager.getPluginsPath() + "/factorio_autocompletion/factorio_prototypes/";
    private static final String prototypeLibPath = prototypeRootPath + "library/";
    private static final String prototypeDownloadLink = "https://factorio-api.knox.moe/prototypes.json";

    private static AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioAutocompletionState config;
    private ProgressIndicator indicator;
    private String saveDir;

    public FactorioPrototypeParser(@Nullable Project project, @NlsContexts.ProgressTitle @NotNull String title, @NotNull String saveDir) {
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
                ProgressManager.getInstance().run(new FactorioPrototypeParser(project, "Download and Parse Factorio Prototypes", prototypeLibPath));
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
                Notification notification = getNotificationGroup().createNotification("Error creating the directories for the Factorio Prototypes.", NotificationType.ERROR);
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

    /**
     * Entry-point for parsing
     * Download the main Prototype-Page and parse all prototypes
     */
    private void downloadAndParsePrototypes() {
        // download and parse json
        JsonRoot jsonPrototype;
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(new URL(prototypeDownloadLink).openStream());
            jsonPrototype = new Gson().fromJson(inputStreamReader, JsonRoot.class);
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(false);
            return;
        }


        try {
            writeTables(jsonPrototype.tables);
            writeAliases(jsonPrototype.aliases);
            writeStrings(jsonPrototype.strings);
            writePrototypes(jsonPrototype.prototypes);
        } catch (IOException e) {
            e.printStackTrace();
            showDownloadingError(true);
            return;
        }
    }

    void writeTables(@NotNull List<Table> tables) throws IOException {
        OutputStreamWriter output;
        output = openFile(prototypeLibPath + "tables1" + ".lua");

        int i = 0;
        int filename = 2;
        for (Table table : tables) {
            ++i;
            if (i >= 25) {
                // open new file to write to
                output.flush();
                output.close();

                output = openFile(prototypeLibPath + "tables" + filename + ".lua");

                ++filename;
                i = 0;
            }
            writeDescLine(output, table.description);
            String link = "[Prototype Definition Wiki](https://wiki.factorio.com" + table.link + ")";
            writeDescLine(output, link);
            writeShape(output, table.name, table.prototype);
            writeObjDef(output, table.name, true);
            output.append(newLine);

            writeProperties(output, table.properties, table.name);

            if (table.parent != null && !table.parent.isEmpty()) {
                String[] parents = table.parent.split(":");
                for (String subParent : parents) {
                    Optional<Table> parentTable = tables.stream().filter(table1 -> table1.name.equals(subParent)).findFirst();
                    if (parentTable.isPresent()) {
                        writeProperties(output, parentTable.get().properties, table.name);
                    }
                }
            }

            output.append(newLine);
        }

        output.flush();
        output.close();
    }

    void writeAliases(@NotNull List<Alias> aliases) throws IOException {
        OutputStreamWriter output = openFile(prototypeLibPath + "aliases.lua");

        for (Alias alias : aliases) {
            writeDescLine(output, alias.description);
            writeAlias(output, alias.name, alias.other);
            output.append(newLine);
        }

        output.flush();
        output.close();
    }

    void writeStrings(@NotNull List<StringType> strings) throws IOException {
        OutputStreamWriter output = openFile(prototypeLibPath + "strings.lua");

        for (StringType string : strings) {
            writeDescLine(output, string.description);
            StringBuilder typeBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : string.value.entrySet()) {
                String type = entry.getKey();
                String desc = entry.getValue();
                if (desc != null && !desc.isEmpty()) {
                    output.append("--- ").append(type).append(": ").append(desc).append(newLine);
                }
                if (typeBuilder.length() > 0) {
                    typeBuilder.append('|');
                }
                typeBuilder.append('"').append(type).append('"');
            }
            writeAlias(output, string.name, typeBuilder.toString());
            output.append(newLine);
        }

        output.flush();
        output.close();
    }

    void writePrototypes(@NotNull List<StringType> prototypes) throws IOException {
        OutputStreamWriter output = openFile(prototypeLibPath + "prototypes.lua");

        for (StringType prototype : prototypes) {
            writeDescLine(output, prototype.description);
            StringBuilder typeBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : prototype.value.entrySet()) {
                String key = entry.getKey();
                String object = entry.getValue();

                if (typeBuilder.length() > 0) {
                    typeBuilder.append("|");
                }
                typeBuilder.append('"').append(key).append('"');

                writePrototype(output, key, object);
            }

            writeShape(output, prototype.name);
            writeObjDef(output, prototype.name, true);
            output.append(newLine);

            writeType(output, typeBuilder.toString(), false);
            writeVarDef(output, "type", prototype.name);
            output.append(newLine);
        }

        output.flush();
        output.close();
    }

    void writeDescLine(@NotNull OutputStreamWriter output, String line) throws IOException {
        if (line != null && !line.isEmpty()) {
            line = line.replace('\n', ' ');
            output.append("--- ").append(line).append(newLine);
        }
    }

    void writeShape(@NotNull OutputStreamWriter output, @NotNull String name, String parent) throws IOException {
        output.append("---@shape ").append(name);
        if (parent != null && !parent.isEmpty()) {
            output.append(" : ").append(parent);
        }
        output.append(newLine);
    }

    void writeShape(OutputStreamWriter output, String name) throws IOException {
        writeShape(output, name, null);
    }

    void writeObjDef(OutputStreamWriter output, String className) throws IOException {
        writeObjDef(output, className, false);
    }

    void writeObjDef(@NotNull OutputStreamWriter output, @NotNull String className, boolean local) throws IOException {
        if (local) {
            output.append("local ");
        }

        output.append(className).append(" = {}").append(newLine);
    }

    void writeVarDef(@NotNull OutputStreamWriter output, String name, String object) throws IOException {
        output.append(object);
        if (!name.startsWith("[")) {
            output.append('.');
        }
        output.append(name).append(" = nil").append(newLine);
    }

    void writeDefault(@NotNull OutputStreamWriter output, String _default) throws IOException {
        if (_default != null && !_default.isEmpty())
            output.append("--- ").append("Default: ").append(_default).append(newLine);
    }

    void writeType(@NotNull OutputStreamWriter output, @NotNull String type, boolean optional) throws IOException {
        output.append("---@type ").append(type);
        if (optional) {
            output.append("|nil");
        }
        output.append(newLine);
    }

    void writeProperties(@NotNull OutputStreamWriter output, @NotNull List<Property> properties, String object) throws IOException {
        for (Property property : properties) {
            writeDescLine(output, property.description);
            writeDefault(output, property._default);
            writeType(output, property.type, property.optional);
            writeVarDef(output, property.name, object);
            output.append(newLine);
        }
    }

    void writeAlias(@NotNull OutputStreamWriter output, @NotNull String alias, @NotNull String type) throws IOException {
        output.append("---@alias ").append(alias).append(' ').append(type).append(newLine);
    }

    void writePrototype(@NotNull OutputStreamWriter output, @NotNull String key, @NotNull String object) throws IOException {
        output.append("---@prototype ").append(key).append(' ').append(object).append(newLine);
    }
}

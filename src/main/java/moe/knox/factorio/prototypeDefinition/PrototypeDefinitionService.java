package moe.knox.factorio.prototypeDefinition;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.downloader.DownloaderContainer;
import moe.knox.factorio.prototypeDefinition.types.Prototype;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.Map;

@Service
final public class PrototypeDefinitionService {
    public static PrototypeDefinitionService getInstance(Project project) {
        return ServiceManager.getService(project, PrototypeDefinitionService.class);
    }

    public PrototypeDefinitionService(Project project) {
        this.project = project;
    }

    Project project;
    Map<String, Prototype> prototypeMap;

    public void reload() {
        String currentPrototypeDefinitionJson = DownloaderContainer.getInstance(project).getCurrentPrototypeDefinitionJson();
        if (currentPrototypeDefinitionJson != null) {
            // read file into correct map format
            FileReader jsonFileReader;
            try {
                jsonFileReader = new FileReader(currentPrototypeDefinitionJson);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                prototypeMap = null;
                return;
            }
            Type mapType = new TypeToken<Map<String, Prototype>>() {}.getType();
            prototypeMap = new GsonBuilder()
                    .registerTypeAdapter(Prototype.class, new Prototype.PrototypeDeserializer())
                    .create()
                    .fromJson(jsonFileReader, mapType);
        } else {
            prototypeMap = null;
        }
    }
}

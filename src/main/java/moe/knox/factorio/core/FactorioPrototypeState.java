package moe.knox.factorio.core;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.serialization.SerializationException;
import com.intellij.util.xmlb.XmlSerializer;
import lombok.CustomLog;
import moe.knox.factorio.core.parser.prototype.PrototypeParser;
import org.jdom.Document;
import org.jdom.Element;

import java.io.File;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

@CustomLog
public class FactorioPrototypeState {
    private static FactorioPrototypeState instance;
    private static final String file = PrototypeParser.prototypeRootPath + "prototype.xml";

    private DataHolder data;

    private FactorioPrototypeState() {
        try {
            data = XmlSerializer.deserialize(new File(file).toURI().toURL(), DataHolder.class);
        } catch (MalformedURLException e) {
            log.error(e);
        } catch (SerializationException e) {
            log.error(e);
            data = new DataHolder();
        }
    }

    public static synchronized FactorioPrototypeState getInstance() {
        if (instance == null) {
            instance = new FactorioPrototypeState();
        }
        return instance;
    }

    public void setKuckuck(String kuckuck) {
        data.kuckuck = kuckuck;
        save();
    }

    public List<String> getPrototypeTypes() {
        return data.prototypeTypes;
    }

    public void setPrototypeTypes(List<String> prototypeTypes) {
        data.prototypeTypes = prototypeTypes;
        save();
    }

    private void save() {
        // serialize data to have it as xml elements
        Element serializedData = XmlSerializer.serialize(data);

        // create xml document from serialized data
        Document document = new Document(serializedData);

        // save document to file
        try {
            JDOMUtil.write(document, "\n");
        } catch (UncheckedIOException e) {
            log.error(e);
        }
    }

    public static class DataHolder {
        public List<String> prototypeTypes;
        public String kuckuck;
        public DataHolder() {
            kuckuck = "";
            prototypeTypes = new ArrayList<>();
        }
    }
}

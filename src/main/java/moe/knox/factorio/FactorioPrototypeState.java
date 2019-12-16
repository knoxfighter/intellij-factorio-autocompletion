package moe.knox.factorio;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.serialization.SerializationException;
import com.intellij.util.xmlb.XmlSerializer;
import moe.knox.factorio.library.FactorioPrototypeParser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class FactorioPrototypeState {
    private static FactorioPrototypeState instance;
    private static String file = FactorioPrototypeParser.prototypeRootPath + "prototype.xml";

    private DataHolder data;

    public static class DataHolder {
        public DataHolder() {
            kuckuck = "";
            prototypeTypes = new ArrayList<>();
        }

        public List<String> prototypeTypes;
        public String kuckuck;
    }

    private FactorioPrototypeState() {
        try {
            data = XmlSerializer.deserialize(new File(file).toURI().toURL(), DataHolder.class);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SerializationException e) {
//            e.printStackTrace();
            data = new DataHolder();
        }
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

        // generate xml outputter
        XMLOutputter outputter = JDOMUtil.createOutputter("\n");

        // save document to file
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            outputter.output(document, fileOutputStream);
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized FactorioPrototypeState getInstance() {
        if (instance == null) {
            instance = new FactorioPrototypeState();
        }
        return instance;
    }
}

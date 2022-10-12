package moe.knox.factorio.api.parser.helper;

import com.google.gson.GsonBuilder;
import marcono1234.gson.recordadapter.RecordTypeAdapterFactory;
import moe.knox.factorio.api.parser.deserializer.postprocessing.PostProcessingTypeAdapterFactory;

public class ParsingHelper {
    public static GsonBuilder getBuilder() {
        return new GsonBuilder()
                .registerTypeAdapterFactory(
                        RecordTypeAdapterFactory.builder()
                                .allowMissingComponentValues()
                                .create()
                )
                .registerTypeAdapterFactory(new PostProcessingTypeAdapterFactory());
    }
}

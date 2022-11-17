package moe.knox.factorio.api.parser.deserializer.postprocessing;

/**
 * Used by the {@link PostProcessingTypeAdapterFactory} to allow Postprocessing types after their deserialization is done.
 */
public interface PostProcessable {
    void postProcess();
}

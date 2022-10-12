package moe.knox.factorio.api.parser.deserializer.postprocessing;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * If this Factory is added to the Gson context, this class calls `postProcess` on all {@link PostProcessable} classes that where deserialized.
 * <br>
 * To add the class to the context:
 * <pre>
 * {@code
 * new GsonBuilder()
 *     .registerTypeAdapterFactory(
 *         new PostProcessingTypeAdapterFactory()
 *     )
 *     .build()
 *     .fromJson(...)
 * }
 * </pre>
 */
public class PostProcessingTypeAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        final TypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);

        return new TypeAdapter<T>() {
            @Override
            public void write(JsonWriter out, T value) throws IOException {
                delegate.write(out, value);
            }

            @Override
            public T read(JsonReader in) throws IOException {
                // read original object
                T obj = delegate.read(in);

                // postprocess the object, if needs be
                if (obj instanceof PostProcessable) {
                    ((PostProcessable) obj).postProcess();
                }

                return obj;
            }
        };
    }
}

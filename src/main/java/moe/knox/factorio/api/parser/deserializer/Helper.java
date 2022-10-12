package moe.knox.factorio.api.parser.deserializer;

import com.google.gson.reflect.TypeToken;
import moe.knox.factorio.api.parser.deserializer.JsonPolymorphism.JsonPolymorphismValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class Helper {
    /**
     * Find a subclass that has the {@link JsonPolymorphismValue} annotation with the correct value.
     *
     * @param val the value the annotation has to have
     * @param type The baseclass TypeToken to iterate over all subclasses
     * @return the class that has the annotation or null if none was found
     */
    public static <T> @Nullable Class<T> getAnnotatedClassForType(String val, @NotNull TypeToken<T> type) {
        // iterate over all subclasses in class
        for (Class<?> declaredClass : type.getRawType().getDeclaredClasses()) {
            // get my annotation
            JsonPolymorphismValue annotation = declaredClass.getAnnotation(JsonPolymorphismValue.class);
            // only run if annotation was found
            if (annotation != null) {
                // check if this annotation defines, that we want to use this class for data
                if (Arrays.stream(annotation.value()).anyMatch(val::equals)) {
                    // check if the class implements the interface T
                    if (((Class) type.getType()).isAssignableFrom(declaredClass)) {
                        return ((Class<T>) declaredClass);
                    }
                }
            }
        }
        return null;
    }
}

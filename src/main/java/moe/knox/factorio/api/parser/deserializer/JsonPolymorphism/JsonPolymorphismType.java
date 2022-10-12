package moe.knox.factorio.api.parser.deserializer.JsonPolymorphism;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by {@link JsonPolymorphismDeserializer} and {@link OptionalJsonPolymorphismDeserializer}.<br>
 * Only mark the baseclass with this. Used subclasses have to be marked with {@link JsonPolymorphismValue}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolymorphismType {
    /**
     * Define the json field that is read to determine the used subclass.
     */
    String value();
}

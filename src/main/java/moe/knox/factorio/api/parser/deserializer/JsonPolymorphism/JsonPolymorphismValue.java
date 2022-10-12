package moe.knox.factorio.api.parser.deserializer.JsonPolymorphism;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used by {@link JsonPolymorphismDeserializer} and {@link OptionalJsonPolymorphismDeserializer}.<br>
 * It can currently only be used on subclasses, that implement/extend the baseclass where {@link JsonPolymorphismType} is also defined on.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonPolymorphismValue {
    String[] value();
}

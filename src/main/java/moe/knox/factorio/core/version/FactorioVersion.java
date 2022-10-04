package moe.knox.factorio.core.version;

import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record FactorioVersion(String version, boolean latest) implements Comparable<FactorioVersion> {
    @Override
    public String toString() {
        return version;
    }

    public static FactorioVersion createVersion(String version)
    {
        return new FactorioVersion(version, false);
    }

    public static FactorioVersion createLatestVersion(String version)
    {
        return new FactorioVersion(version, true);
    }

    @Override
    public int compareTo(@NotNull FactorioVersion o) {
        SemVer verA = Objects.requireNonNull(SemVer.parseFromText(version));
        SemVer verB = Objects.requireNonNull(SemVer.parseFromText(o.version));

        return verA.compareTo(verB);
    }
}

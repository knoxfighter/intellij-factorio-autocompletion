package moe.knox.factorio.core.version;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class FactorioVersionCollection extends TreeSet<FactorioVersion> {
    @NotNull
    public FactorioVersion latestVersion()
    {
        return stream()
                .filter(FactorioVersion::latest)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::max));
    }
}

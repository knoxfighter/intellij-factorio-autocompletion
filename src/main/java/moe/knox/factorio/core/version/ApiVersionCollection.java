package moe.knox.factorio.core.version;

import java.util.Collections;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class ApiVersionCollection extends TreeSet<FactorioApiVersion> {
    public FactorioApiVersion latestVersion()
    {
        return stream()
                .filter(FactorioApiVersion::latest)
                .collect(Collectors.collectingAndThen(Collectors.toSet(), Collections::max));
    }
}

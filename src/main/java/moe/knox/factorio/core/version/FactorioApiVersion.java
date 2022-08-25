package moe.knox.factorio.core.version;

public record FactorioApiVersion(String version, boolean latest) {
    private static final String latestVersion = "latest";

    @Override
    public String toString() {
        if (latest) {
            return "Latest version";
        }

        return version;
    }

    public static FactorioApiVersion createLatest()
    {
        return new FactorioApiVersion(latestVersion, true);
    }

    public static FactorioApiVersion createVersion(String version)
    {
        return new FactorioApiVersion(version, false);
    }
}

package moe.knox.factorio.core.version;

import com.intellij.util.text.SemVer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Return collection of supported api versions
 * @see FactorioApiVersion
 */
public final class ApiVersionResolver {
    final private SemVer minimalSupportedVersion = new SemVer("1.1.62", 1, 1, 62);
    final private SemVer maximalSupportedVersion = new SemVer("1.2.0", 1, 2, 0);
    final private static String versionsHtmlPage = "https://lua-api.factorio.com/";

    public ApiVersionCollection supportedVersions() throws IOException {
        TreeSet<SemVer> allSupportedVersions = allVersions()
                .stream()
                .filter(this::isSupported)
                .collect(Collectors.toCollection(TreeSet::new));
        SemVer lastSupportedVersion = Objects.requireNonNull(allSupportedVersions.last());
        var collection = new ApiVersionCollection();

        for (SemVer v : allSupportedVersions) {
            if (v.equals(lastSupportedVersion)) {
                collection.add(FactorioApiVersion.createLatestVersion(v.toString()));
            } else {
                collection.add(FactorioApiVersion.createVersion(v.toString()));
            }
        }

        return collection;
    }

    private boolean isSupported(SemVer version) {
        return version.compareTo(minimalSupportedVersion) > 0 && version.compareTo(maximalSupportedVersion) < 0;
    }

    private Set<SemVer> allVersions()  throws IOException
    {
        var versions = new TreeSet<SemVer>();

        Document mainPageDoc = Jsoup.connect(versionsHtmlPage).get();
        Elements allLinks = mainPageDoc.select("a");
        for (Element link : allLinks) {
            var semVer = SemVer.parseFromText(link.text());
            if (semVer == null) {
                continue;
            }

            versions.add(semVer);
        }

        return versions;
    }
}

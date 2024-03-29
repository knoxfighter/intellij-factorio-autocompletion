package moe.knox.factorio.core.version;

import com.intellij.util.text.SemVer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Return collection of supported api versions
 * @see FactorioApiVersion
 */
public final class ApiVersionResolver {
    final private SemVer minimalSupportedVersion = new SemVer("1.1.62", 1, 1, 62);
    final private static String versionsHtmlPage = "https://lua-api.factorio.com/";

    public ApiVersionCollection supportedVersions() throws IOException {
        var allVersions = getAllVersions();
        var lastVersion = Collections.max(allVersions);
        var supportedVersions = new ApiVersionCollection();

        for (SemVer version : allVersions) {
            if (!version.isGreaterOrEqualThan(minimalSupportedVersion)) {
                continue;
            }

            FactorioApiVersion factorioVersion;

            if (version.equals(lastVersion)) {
                factorioVersion = FactorioApiVersion.createLatestVersion(version.getRawVersion());
            } else {
                factorioVersion = FactorioApiVersion.createVersion(version.getRawVersion());
            }

            supportedVersions.add(factorioVersion);
        }

        return supportedVersions;
    }

    private Set<SemVer> getAllVersions()  throws IOException
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

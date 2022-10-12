package moe.knox.factorio.api.parser;

import moe.knox.factorio.api.parser.data.RuntimeApi;
import moe.knox.factorio.api.parser.helper.ParsingHelper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

public class Parser {
    private static String BASE_LINK = "https://lua-api.factorio.com/%s/runtime-api.json";

    public static @NotNull RuntimeApi fromWebsite(String version) throws IOException {
        // build download link
        String apiLink = String.format(BASE_LINK, version);

        // download
        InputStreamReader inputStreamReader = new InputStreamReader(new URL(apiLink).openStream());

        RuntimeApi runtimeApi = ParsingHelper.getBuilder().create().fromJson(inputStreamReader, RuntimeApi.class);

        if (runtimeApi.apiVersion < 1 || runtimeApi.apiVersion > 3) {
            throw new UnsupportedOperationException("This version of the json is not supported");
        }

        return runtimeApi;
    }
}

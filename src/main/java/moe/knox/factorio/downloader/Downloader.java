package moe.knox.factorio.downloader;

interface Downloader {
    void cancel();

    /**
     * @param version
     * @return true if download got started (false if a download is already running)
     */
    boolean download(String version);
}

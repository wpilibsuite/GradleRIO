package edu.wpi.first.gradlerio.wpi.dependencies

import de.undercouch.gradle.tasks.download.DownloadAction
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * A task type for downloading vendordep JSON files from the vendor URL.
 */
class VendorDepTask extends DefaultTask{
    private String url
    private DownloadAction da = new DownloadAction(getProject())

    @Option(option = "url", description = "The vendordep JSON URL.")
    void setURL(String url) {
        this.url = url
    }

    /**
     * Find the name of the JSON file.
     * @param url the vendor JSON URL
     * @return the name of the JSON file, with the `.json` suffix
     */
    private static String findFileName(String url) {
        if (url == null) {
            throw new IllegalArgumentException(
                    "No vendor JSON URL was entered. Try the following:\n\tgradlew vendordep --url=<insert_url_here>")
        }
        int lastUrlSeparator = url.lastIndexOf('/')
        if (lastUrlSeparator == -1) {
            throw new IllegalArgumentException(
                    "No vendor JSON URL was entered. Try the following:\n\tgradlew vendordep --url=<insert_url_here>")
        }
        String name = url.substring(lastUrlSeparator + 1)
        return "vendordeps/${name}"
    }

    /**
     * Installs the JSON file
     */
    @TaskAction
    void install() throws IOException {
        da.src(url)
        da.dest(findFileName(url))
        da.execute()
    }
}

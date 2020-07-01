package edu.wpi.first.gradlerio.wpi.dependencies

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

/**
 * A task type for downloading vendordep JSON files from the vendor URL.
 */
class VendorDepTask extends Exec{
    private String url
//    private String file

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
        int lastUrlSeparator = url.lastIndexOf('/')
        if(lastUrlSeparator == -1) {
            throw new IllegalArgumentException("No vendor JSON URL was entered.")
        }
        String name = url.substring(lastUrlSeparator + 1)
        return "vendordeps/${name}"
    }

    /**
     * Installs the JSON file
     */
    @TaskAction
    def install() {
        commandLine 'curl', '-o', findFileName(url), url
    }

}
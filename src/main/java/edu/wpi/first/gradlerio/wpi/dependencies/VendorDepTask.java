package edu.wpi.first.gradlerio.wpi.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import de.undercouch.gradle.tasks.download.DownloadAction;
import edu.wpi.first.gradlerio.wpi.WPIExtension;

/**
 * A task type for downloading vendordep JSON files from the vendor URL.
 */
public class VendorDepTask extends DefaultTask {
    private String url;
    private DownloadAction downloadAction = new DownloadAction(getProject());
    private WPIExtension wpiExt = getProject().getExtensions().getByType(WPIExtension.class);

    @Option(option = "url", description = "The vendordep JSON URL or path")
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Installs the JSON file
     */
    @TaskAction
    public void install() throws IOException {
        String filename = findFileName(url);
        Path dest = computeDest(filename);
        if (url.startsWith("FRCLOCAL/")) {
            getLogger().info("Locally fetching $filename");
            copyLocal(filename, dest);
        } else {
            getLogger().info("Remotely fetching " + filename);
            downloadRemote(dest);
        }
    }

    /**
     * Find the name of the JSON file.
     * @param inputUrl the vendor JSON URL
     * @return the name of the JSON file, with the `.json` suffix
     */
    private static String findFileName(String inputUrl) {
        if (inputUrl == null) {
            throw new IllegalArgumentException(
                    "No valid vendor JSON URL was entered. Try the following:\n\tgradlew vendordep --url=<insert_url_here>\n" +
                            "Use either a URL to fetch a remote JSON file or `FRCLOCAL/Filename.json` to fetch from the local wpilib folder."
            );
        }
        int lastUrlSeparator = inputUrl.lastIndexOf('/');
        if (lastUrlSeparator == -1) {
            throw new IllegalArgumentException(
                    "Invalid vendor JSON URL was entered. Try the following:\n\tgradlew vendordep --url=<insert_url_here>\n" +
                            "Use either a URL to fetch a remote JSON file or `FRCLOCAL/Filename.json` to fetch from the local wpilib folder."
            );
        }
        return inputUrl.substring(lastUrlSeparator + 1);
    }

    private Path computeDest(String filename) {
        Object property = getProject().findProperty(WPIVendorDepsExtension.GRADLERIO_VENDOR_FOLDER_PROPERTY);
        // find project vendordeps folder
        String destfolder = property != null ? (String)property : WPIVendorDepsExtension.DEFAULT_VENDORDEPS_FOLDER_NAME;

        return getProject().file(destfolder).toPath().resolve(filename);
    }

    /**
     * Fetch and copy a vendor JSON from `FRCHOME/vendordeps`
     * @param filename the vendor JSON file name
     * @param dest the destination file
     */
    private void copyLocal(String filename, Path dest) {
        Path localCache = Path.of(wpiExt.getFrcHome()).resolve("vendordeps");
        File localFolder = localCache.toFile();
        if (!localFolder.isDirectory()) {
            getLogger().error("For some reason " + localFolder + " is not a folder");
            return;
        }

        File[] matches = localFolder.listFiles((dir, name) -> {
            return name == filename;
        });

        // no matches means that source file doesn't exist
        if (matches.length < 1) {
            getLogger().error("Vendordep file " + filename + " was not found in local wpilib vendordep folder (" + localCache.toString() + ").");
            return;
        }

        // only one match could have been found
        Path src = matches[0].toPath();
        getLogger().info("Copying file " + filename + " from " + src.toString() + " to " + dest.toString());
        try {
            if (dest.toFile().exists()) {
                getLogger().warn("Destination file " + filename + " exists and is being overwritten.");
            }
            Path result = Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            getLogger().info("Successfully copied " + filename + " to " + result);
        } catch (IOException ex) {
            getLogger().error(ex.toString());
        }
    }

    /**
     * Download a vendor JSON file from a URL
     * @param dest the destination file
     */
    private void downloadRemote(Path dest) throws IOException {
        downloadAction.src(url);
        downloadAction.dest(dest.toFile());
        downloadAction.execute();
    }
}

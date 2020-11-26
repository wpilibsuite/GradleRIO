package edu.wpi.first.gradlerio.wpi.dependencies

import de.undercouch.gradle.tasks.download.DownloadAction
import edu.wpi.first.gradlerio.wpi.WPIExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * A task type for downloading vendordep JSON files from the vendor URL.
 */
class VendorDepTask extends DefaultTask {
    private String url
    private DownloadAction downloadAction = new DownloadAction(getProject())
    private wpiExt = project.getExtensions().getByType(WPIExtension)

    @Option(option = "url", description = "The vendordep JSON URL or path")
    void setURL(String url) {
        this.url = url
    }

    /**
     * Installs the JSON file
     */
    @TaskAction
    void install() throws IOException {
        String filename = findFileName(url)
        Path dest = computeDest(filename)
        if (url.startsWith("FRCLOCAL/")) {
            logger.info("Locally fetching $filename")
            copyLocal(filename, dest)
        } else {
            logger.info("Remotely fetching $filename")
            downloadRemote(dest)
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
            )
        }
        int lastUrlSeparator = inputUrl.lastIndexOf('/')
        if (lastUrlSeparator == -1) {
            throw new IllegalArgumentException(
                    "Invalid vendor JSON URL was entered. Try the following:\n\tgradlew vendordep --url=<insert_url_here>\n" +
                            "Use either a URL to fetch a remote JSON file or `FRCLOCAL/Filename.json` to fetch from the local wpilib folder."
            )
        }
        return inputUrl.substring(lastUrlSeparator + 1)
    }

    private Path computeDest(String filename) {
        // find project vendordeps folder
        String destfolder =
                project.findProperty(WPIVendorDepsExtension.GRADLERIO_VENDOR_FOLDER_PROPERTY) ?:
                        WPIVendorDepsExtension.DEFAULT_VENDORDEPS_FOLDER_NAME

        return project.file(destfolder).toPath().resolve(filename)
    }

    /**
     * Fetch and copy a vendor JSON from `FRCHOME/vendordeps`
     * @param filename the vendor JSON file name
     * @param dest the destination file
     */
    private void copyLocal(String filename, Path dest) {
        Path localCache = Path.of(wpiExt.getFrcHome()).resolve("vendordeps")
        File localFolder = localCache.toFile()
        if (!localFolder.isDirectory()) {
            logger.error("For some reason $localFolder is not a folder")
            return
        }

        List<File> matches = localFolder.listFiles(new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                return name == filename
            }
        }) as List<File>

        // no matches means that source file doesn't exist
        if (matches.size() < 1) {
            logger.error("Vendordep file $filename was not found in local wpilib vendordep folder (${localCache.toString()}).")
            return
        }

        // only one match could have been found
        Path src = matches[0].toPath()
        logger.info("Copying file $filename from ${src.toString()} to ${dest.toString()}")
        try {
            if (dest.toFile().exists()) {
                logger.warn("Destination file $filename exists and is being overwritten.")
            }
            Path result = Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
            logger.info("Successfully copied $filename to $result")
        } catch (IOException ex) {
            logger.error(ex.toString())
        }
    }

    /**
     * Download a vendor JSON file from a URL
     * @param dest the destination file
     */
    private void downloadRemote(Path dest) {
        downloadAction.src(url)
        downloadAction.dest(dest.toFile())
        downloadAction.execute()
    }
}

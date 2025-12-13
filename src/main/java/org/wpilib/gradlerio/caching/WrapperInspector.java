package org.wpilib.gradlerio.caching;

import org.wpilib.deployutils.log.ETLogger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.gradle.api.Project;
import org.gradle.api.tasks.wrapper.Wrapper;
import org.gradle.wrapper.WrapperExecutor;

public class WrapperInspector {

    public static final String NAME = "wrapper";

    public static void run(Project project, ETLogger log) {
        File settingsFile = project.file("gradle/wrapper/gradle-wrapper.properties");
        boolean wrapperTaskQueued = project.getGradle().getTaskGraph().getAllTasks().stream().filter(x -> x instanceof Wrapper).findAny().isPresent();
        if (settingsFile.exists() && settingsFile.isFile() && !wrapperTaskQueued) {
            log.info("Found gradle-wrapper.properties on " + project.getPath());
            Properties props = new Properties();
            try (FileReader reader = new FileReader(settingsFile)) {
                props.load(reader);
                if (requiresUpdate(props)) {
                    log.logErrorHead("Warning! Your wrapper zip / dist store is set to wrapper/dists!");
                    log.logError("This can cause issues when going to competition, since this directory is automatically purged once a month");
                    log.logError("Run ./gradlew wrapper to fix this, or use -Pskip-inspector-wrapper to squash this warning.");
                }
                reader.close();
            } catch (IOException e) {
                log.info("Wrapper Inspector failed: " + e.getMessage());
            }
        }
    }

    public static boolean requiresUpdate(Properties props) {
        String zipStorePath = props.getProperty(WrapperExecutor.ZIP_STORE_PATH_PROPERTY);
        String distributionPath = props.getProperty(WrapperExecutor.DISTRIBUTION_PATH_PROPERTY);
        boolean isZipStoreValid = zipStorePath != null && zipStorePath.equals("wrapper/dists");
        boolean isDistributionPathValid = distributionPath != null && distributionPath.equals("wrapper/dists");
        return isZipStoreValid || isDistributionPathValid;
    }

}

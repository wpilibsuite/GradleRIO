package edu.wpi.first.gradlerio.caching

import groovy.transform.CompileStatic
import jaci.gradle.log.ETLogger
import org.gradle.api.Project
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.wrapper.WrapperExecutor

@CompileStatic
class WrapperInspector {

    public static final String NAME = "wrapper"

    public static void run(Project project, ETLogger log) {
        def settingsFile = project.file('gradle/wrapper/gradle-wrapper.properties')
        def wrapperTaskQueued = !project.gradle.taskGraph.getAllTasks().findAll { it instanceof Wrapper }.empty
        if (settingsFile.exists() && settingsFile.isFile() && !wrapperTaskQueued) {
            log.info("Found gradle-wrapper.properties on ${project.path}")
            Properties props = new Properties()
            FileReader reader = new FileReader(settingsFile)
            try {
                props.load(reader)
                if (requiresUpdate(props)) {
                    log.logErrorHead("Warning! Your wrapper zip / dist store is set to wrapper/dists!")
                    log.logError("This can cause issues when going to competition, since this directory is automatically purged once a month")
                    log.logError("Run ./gradlew wrapper to fix this, or use -Pskip-inspector-wrapper to squash this warning.")
                }
                reader.close()
            } catch (IOException e) {
                log.info("Wrapper Inspector failed: ${e.message}")
            }
        }
    }

    public static boolean requiresUpdate(Properties props) {
        return props.getProperty(WrapperExecutor.ZIP_STORE_PATH_PROPERTY) == "wrapper/dists" || props.getProperty(WrapperExecutor.DISTRIBUTION_PATH_PROPERTY) == "wrapper/dists"
    }

}

package edu.wpi.first.gradlerio.test;

import java.io.File;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.jvm.tasks.Jar;

public class ExtractTestJNITask extends DefaultTask {
    private Jar jar;

    @Internal
    public Jar getJar() {
        return jar;
    }

    public void setJar(Jar jar) {
        this.jar = jar;
    }

    private FileCollection extractedFiles = null;

    @TaskAction
    public void extract() {
        // Extract necessary libs
        Configuration nativeLibs = getProject().getConfigurations().getByName("nativeDesktopLib");
        Configuration nativeZips = getProject().getConfigurations().getByName("nativeDesktopZip");

        nativeLibs.getDependencies().matching(new Spec<Dependency>() {
            @Override
            public boolean isSatisfiedBy(Dependency dep) {
                return dep != null && nativeLibs.files(dep).size() > 0;
            }
        })
        .all(new Action<Dependency>() {
            @Override
            public void execute(Dependency dep) {
                ConfigurableFileCollection fc = getProject().files(nativeLibs.files(dep).toArray());
                if (extractedFiles == null) {
                    extractedFiles = fc;
                } else {
                    extractedFiles = extractedFiles.plus(fc);
                }
            }
        });

        nativeZips.getDependencies().matching(new Spec<Dependency>() {
            @Override
            public boolean isSatisfiedBy(Dependency dep) {
                return dep != null && nativeZips.files(dep).size() > 0;
            }
        })
        .all(new Action<Dependency>() {
            @Override
            public void execute(Dependency dep) {
                FileTree zipTree = getProject().zipTree(nativeZips.files(dep).iterator().next());
                String[] matchers = new String[] {"**/*.so*", "**/*.so", "**/*.dll", "**/*.dylib"};
                FileTree fc = zipTree.matching(new Action<PatternFilterable>() {
                    @Override
                    public void execute(PatternFilterable pat) {
                        pat.include(matchers);
                    }
                });
                if (extractedFiles == null) {
                    extractedFiles = fc;
                } else {
                    extractedFiles = extractedFiles.plus(fc);
                }
            }
        });

        // TODO Add me back in
        // File dir = JavaTestPlugin.jniExtractionDir(project)
        // if (dir.exists()) dir.deleteDir()
        // dir.parentFile.mkdirs()

        // if (extractedFiles != null) {
        //     project.copy { CopySpec s ->
        //         s.from(project.files { extractedFiles.files })
        //         s.into(dir)
        //     }
        // }
    }
}

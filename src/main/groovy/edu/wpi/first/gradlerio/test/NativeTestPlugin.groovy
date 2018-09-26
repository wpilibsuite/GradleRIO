package edu.wpi.first.gradlerio.test

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.model.Mutate
import org.gradle.model.RuleSource
import org.gradle.nativeplatform.test.googletest.GoogleTestTestSuiteBinarySpec
import org.gradle.platform.base.BinaryContainer

class NativeTestPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) { }

    static class NativeTestRules extends RuleSource {
        @Mutate
        void addBinaryFlags(BinaryContainer binaries) {
            binaries.withType(GoogleTestTestSuiteBinarySpec) { GoogleTestTestSuiteBinarySpec bin ->
                if (!bin.targetPlatform.name.equals('desktop'))
                    bin.buildable = false

                bin.cppCompiler.define('RUNNING_FRC_TESTS')
            }
        }
    }
}

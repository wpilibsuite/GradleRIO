package edu.wpi.first.gradlerio.frc

import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import jaci.gradle.deploy.target.RemoteTarget

@CompileStatic
@InheritConstructors
abstract class FRCCompatibleTarget extends RemoteTarget { }
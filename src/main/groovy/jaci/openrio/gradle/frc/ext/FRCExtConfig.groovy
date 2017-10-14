package jaci.openrio.gradle.frc.ext

import groovy.transform.CompileStatic
import jaci.gradle.deployers.Deployer

@CompileStatic
class FRCExtConfig {

    public String name
    public Closure<Deployer> deployer
    public List<String> targets = []

    public Closure robotCommand

    public FRCExtConfig(String name) {
        this.name = name
    }

    def deployer(Closure<Deployer> deployer) {
        this.deployer = deployer
    }

    def roborio(String roborioName) {
        targets << roborioName
    }

    def robotCommand(Closure robotCommandClosure) {
        robotCommand = robotCommandClosure
    }

}

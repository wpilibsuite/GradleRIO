package jaci.openrio.gradle.frc

import jaci.gradle.deployers.Deployer

class FRCDeployer {

    public String name
    public Closure<Deployer> deployer
    public List<String> targets = []

    public Closure robotCommand

    public FRCDeployer(String name) {
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

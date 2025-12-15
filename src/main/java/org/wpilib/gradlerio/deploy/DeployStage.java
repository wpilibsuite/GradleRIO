package org.wpilib.gradlerio.deploy;

public enum DeployStage {
    BeforeProgramKill,
    ProgramKill,
    AfterProgramKill,
    FileRetreival,
    FileDeploy,
    BeforeProgramStart,
    ProgramStart,
    AfterProgramStart
}

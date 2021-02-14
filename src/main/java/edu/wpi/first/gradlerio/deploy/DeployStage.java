package edu.wpi.first.gradlerio.deploy;

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

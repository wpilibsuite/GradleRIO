package edu.wpi.first.gradlerio.deploy;

public enum DeployStage {
    BeforeProgramKill,
    ProgramKill,
    AfterProgramKill,
    FileRetreival,
    FileClear,
    FileDeploy,
    BeforeProgramStart,
    ProgramStart,
    AfterProgramStart
}

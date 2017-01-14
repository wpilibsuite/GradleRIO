package test.frc;

import edu.wpi.first.wpilibj.IterativeRobot;
import test.frc.lib.FrcLibrary;

import java.util.Random;

public class TestRobot extends IterativeRobot {

    static Random random = new Random();

    @Override
    public void robotInit() {
        super.robotInit();
        FrcLibrary.libraryInit();
    }

    @Override
    public void teleopPeriodic() {
        FrcLibrary.getTalon().set(random.nextDouble() * 2 - 1);
    }
}

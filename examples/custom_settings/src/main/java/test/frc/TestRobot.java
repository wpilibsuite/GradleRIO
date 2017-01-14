package test.frc;

import com.ctre.CANTalon;
import edu.wpi.first.wpilibj.IterativeRobot;

import java.util.Random;

public class TestRobot extends IterativeRobot {

    static CANTalon talon;
    static Random random = new Random();

    @Override
    public void robotInit() {
        super.robotInit();
        System.out.println("Hello World");
        talon = new CANTalon(1);
    }

    @Override
    public void teleopPeriodic() {
        talon.set(random.nextDouble() * 2 - 1);
    }
}

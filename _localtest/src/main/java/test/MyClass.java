package test;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.IterativeRobot;

public class MyClass extends IterativeRobot {

    public MyClass() {
        super();
    }

    public void robotInit() {
        System.out.println("Hello World");
    }

    @Override
    public void teleopPeriodic() {
    }

    public int onePlusOne() {
        return 2;
    }
}
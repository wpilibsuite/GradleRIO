package test;
import com.ctre.phoenix.MotorControl.CAN.TalonSRX;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Utility;

public class MyClass extends IterativeRobot {
    TalonSRX talon;

    public void robotInit() {
        talon = new TalonSRX(99);
        System.out.println("Hello World");
        System.out.print(Utility.getFPGATime());
    }
}
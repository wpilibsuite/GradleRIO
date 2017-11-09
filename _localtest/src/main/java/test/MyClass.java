package test;
import com.ctre.phoenix.MotorControl.CAN.TalonSRX;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Utility;

public class MyClass extends IterativeRobot {

    AHRS ahrs;

    public void robotInit() {
        TalonSRX srx = new TalonSRX(99);
        ahrs = new AHRS(I2C.Port.kOnboard);
        System.out.println("Hello World");
        System.out.println(Utility.getFPGATime());
    }

    @Override
    public void teleopPeriodic() {
        System.out.println(ahrs.getAngle());
    }
}
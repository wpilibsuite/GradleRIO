package test;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.I2C;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.Utility;

public class MyClass extends IterativeRobot {

    AHRS ahrs;
//    TalonSRX talonSrx;

    public void robotInit() {
        ahrs = new AHRS(I2C.Port.kOnboard);
        System.out.println("Hello World");
        System.out.println(Utility.getFPGATime());

//        talonSrx = new TalonSRX(60);
    }

    @Override
    public void teleopPeriodic() {
        System.out.println(ahrs.getAngle());
//        talonSrx.set(1.0);
    }
}
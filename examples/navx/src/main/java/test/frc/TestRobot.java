package test.frc;

import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.IterativeRobot;
import edu.wpi.first.wpilibj.SPI;

public class TestRobot extends IterativeRobot {

    static AHRS navx;

    @Override
    public void robotInit() {
        super.robotInit();
        navx = new AHRS(SPI.Port.kMXP);
    }
}

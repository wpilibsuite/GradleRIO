import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj.Talon;
import edu.wpi.first.wpilibj.sim.PWMSim;

public class TalonTest {
  @Test
  public void TestTalonSet() {
    RobotBase.initializeHardwareConfiguration();
    try (Talon talon = new Talon(3)) {
      PWMSim talonSim = new PWMSim(3);
      talon.set(0.5);
      assertEquals(0.5, talonSim.getSpeed(), 0.001);
      talon.set(-0.5);
      assertEquals(-0.5, talonSim.getSpeed(), 0.001);
    }
  }
}

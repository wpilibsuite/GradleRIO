import edu.wpi.first.wpilibj.RobotBase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import test.MyClass;

@RunWith(JUnit4.class)
public class TestMyClass {

    @Test
    public void testOnePlusOne() {
//        RobotBase.initializeHardwareConfiguration();
        Assert.assertEquals(new MyClass().onePlusOne(), 2);
    }

}
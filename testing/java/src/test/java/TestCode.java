import org.junit.Test;
import org.wpilib.vision.camera.CameraServerJNI;

public class TestCode {
    @Test
    public void jniLinkTest() {
        CameraServerJNI.getHostname();
    }
}

import org.junit.Test
import org.wpilib.vision.camera.CameraServerJNI

class TestCode {
    @Test
    fun jniLinkTest() {
        CameraServerJNI.getHostname()
    }
}

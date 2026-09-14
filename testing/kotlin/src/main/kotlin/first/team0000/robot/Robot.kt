package first.team0000.robot

import org.wpilib.drive.DifferentialDrive
import org.wpilib.drivers.motor.PWMSparkMax
import org.wpilib.driverstation.Gamepad
import org.wpilib.framework.TimedRobot
import org.wpilib.system.Timer

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
class Robot : TimedRobot() {

    private var leftDrive = PWMSparkMax(0);
    private var rightDrive = PWMSparkMax(1);
    private var robotDrive =
      DifferentialDrive(leftDrive::setThrottle, rightDrive::setThrottle);
    private var controller = Gamepad(0);
    private var timer = Timer();

    init {
      // We need to invert one side of the drivetrain so that positive voltages
      // result in both sides moving forward. Depending on how your robot's
      // gearbox is constructed, you might have to invert the left side instead.
      rightDrive.setInverted(true);
    }

    /**
     * This function is called every robot packet, no matter the mode. Use
     * this for items like diagnostics that you want ran during disabled,
     * autonomous, teleoperated and test.
     *
     * This runs after the mode specific periodic functions, but before
     * LiveWindow and SmartDashboard integrated updating.
     */
    override fun robotPeriodic() {
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different autonomous modes using the dashboard. The sendable
     * chooser code works with the Java SmartDashboard. If you prefer the
     * LabVIEW Dashboard, remove all of the chooser code and uncomment the
     * getString line to get the auto name from the text box below the Gyro
     *
     * You can add additional auto modes by adding additional comparisons to
     * the switch structure below with additional strings. If using the
     * SendableChooser make sure to add them to the chooser code above as well.
     */
    override fun autonomousInit() {
        timer.restart();
    }

    /**
     * This function is called periodically during autonomous.
     */
    override fun autonomousPeriodic() {
      // Drive for 2 seconds
      if (timer.get() < 2.0) {
        // Drive forwards half velocity, make sure to turn input squaring off
        robotDrive.arcadeDrive(0.5, 0.0, false);
      } else {
        robotDrive.arcadeDrive(0.0, 0.0, false); // stop robot
      }
    }

    /**
     * This function is called periodically during operator control.
     */
    override fun teleopPeriodic() {
        robotDrive.arcadeDrive(-controller.getLeftY(), -controller.getRightX());
    }

    /**
     * This function is called periodically during utility mode.
     */
    override fun utilityPeriodic() {
    }

}

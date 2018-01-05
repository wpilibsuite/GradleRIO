#include "WPILib.h"
#include "ctre/phoenix/MotorControl/CAN/WPI_TalonSRX.h"
#include "AHRS.h"

#include <iostream>

using namespace frc;

class Robot: public IterativeRobot {
public:
    Robot() { }
    ctre::phoenix::motorcontrol::can::WPI_TalonSRX *srx;
    AHRS *navx;

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
        srx = new ctre::phoenix::motorcontrol::can::WPI_TalonSRX(60);
        navx = new AHRS(frc::I2C::Port::kMXP);
    }

    void DisabledInit() { }
    void AutonomousInit() {
        std::cout << "Auto" << std::endl;
        srx->Set(1.0);
    }
    void TeleopInit() { }
    void TestInit() { }

    void DisabledPeriodic() { }
    void AutonomousPeriodic() { }
    void TeleopPeriodic() { }
    void TestPeriodic() { }
};

START_ROBOT_CLASS(Robot)
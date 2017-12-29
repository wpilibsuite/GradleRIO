#include "WPILib.h"
//#include "ctre/phoenix/MotorControl/CAN/TalonSRX.h"

#include <iostream>

using namespace frc;

class Robot: public IterativeRobot {
public:
    Robot() { }
//    CTRE::MotorControl::CAN::TalonSRX *srx;

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
//        srx = new CTRE::MotorControl::CAN::TalonSRX(60);
    }

    void DisabledInit() { }
    void AutonomousInit() {
        std::cout << "Auto" << std::endl;
//        srx->Set(1.0);
    }
    void TeleopInit() { }
    void TestInit() { }

    void DisabledPeriodic() { }
    void AutonomousPeriodic() { }
    void TeleopPeriodic() { }
    void TestPeriodic() { }
};

START_ROBOT_CLASS(Robot)
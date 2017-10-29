#include "WPILib.h"
#include "ctre/phoenix/MotorControl/CAN/TalonSRX.h"

#include <iostream>

using namespace frc;

class Robot: public IterativeRobot {
public:
    Robot() { }

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
		CTRE::MotorControl::CAN::TalonSRX talon(99);
		talon.Set(0.5);
    }

    void DisabledInit() { }
    void AutonomousInit() {
        std::cout << "Auto" << std::endl;
    }
    void TeleopInit() { }
    void TestInit() { }

    void DisabledPeriodic() { }
    void AutonomousPeriodic() { }
    void TeleopPeriodic() { }
    void TestPeriodic() { }
};

START_ROBOT_CLASS(Robot)
#include "WPILib.h"

#include <iostream>

using namespace frc;

class Robot: public IterativeRobot {
public:
    Robot() { }

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
    }

    void DisabledInit() { }
    void AutonomousInit() {
    }
    void TeleopInit() { }
    void TestInit() { }

    void DisabledPeriodic() { }
    void AutonomousPeriodic() { }
    void TeleopPeriodic() { }
    void TestPeriodic() { }
};

START_ROBOT_CLASS(Robot)

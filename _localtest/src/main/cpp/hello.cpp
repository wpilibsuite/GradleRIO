#include "WPILib.h"
#include "AHRS.h"
#include "ctrlib/CANTalon.h"

#include <iostream>

using namespace frc;

int add(int a, int b) {
    return a + b;
}

class Robot: public IterativeRobot {
public:
    AHRS *ahrs;
    CANTalon *canTalon;


    Robot() { }

    void RobotInit() {
        std::cout << "Hello World" << std::endl;
        std::cout << add(1, 2) << std::endl;
    }

    void DisabledInit() { }
    void AutonomousInit() { }
    void TeleopInit() { }
    void TestInit() { }

    void DisabledPeriodic() { }
    void AutonomousPeriodic() { }
    void TeleopPeriodic() { }
    void TestPeriodic() { }
};

START_ROBOT_CLASS(Robot)
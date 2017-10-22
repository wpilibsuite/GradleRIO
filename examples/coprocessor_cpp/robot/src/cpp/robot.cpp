#include "WPILib.h"

#include <iostream>

using namespace frc;

class Robot: public IterativeRobot {
public:
    Robot() { }

    void RobotInit() {
        std::cout << "Hello World!" << std::endl;
    }
};

START_ROBOT_CLASS(Robot)
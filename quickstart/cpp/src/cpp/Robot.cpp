#include "WPILib.h"
#include "MyHeader.h"

#ifdef RUNNING_GTEST
#include "HAL/HAL.h"
#include "gtest/gtest.h"
#endif

using namespace frc;

// From MyHeader.h
int add(int a, int b) {
    return a + b;
}

class Robot: public IterativeRobot {
public:
    Robot() { }

    void RobotInit() { }

    void DisabledInit() { }
    void AutonomousInit() { }
    void TeleopInit() { }
    void TestInit() { }

    void DisabledPeriodic() { }
    void AutonomousPeriodic() { }
    void TeleopPeriodic() { }
    void TestPeriodic() { }
};

#ifndef RUNNING_GTEST
int main() { return frc::StartRobot<Robot>(); }
#else
int main(int argc, char** argv) {
    HAL_Initialize(500, 0);
    ::testing::InitGoogleTest(&argc, argv);
    int ret = RUN_ALL_TESTS();
    return ret;
}
#endif

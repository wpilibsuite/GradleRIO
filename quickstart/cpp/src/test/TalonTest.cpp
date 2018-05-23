#include "gtest/gtest.h"
#include "HAL/HAL.h"
#include "Simulation/PWMSim.h"
#include "Talon.h"

TEST(TalonTests, TestTalonSet) {
  frc::Talon talon{3};
  frc::sim::PWMSim talonSim{3};
  talon.Set(0.5);
  ASSERT_DOUBLE_EQ(0.5, talonSim.GetSpeed());
  talon.Set(-0.5);
  ASSERT_DOUBLE_EQ(-0.5, talonSim.GetSpeed());
}



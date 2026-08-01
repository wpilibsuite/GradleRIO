/*----------------------------------------------------------------------------*/
/* Copyright (c) 2015-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FIRST teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

#include <string_view>

#include <catch2/catch_session.hpp>
#include <wpi/hal/HAL.h>

namespace {

bool IsCatchListCommand(int argc, char** argv) {
  for (int i = 1; i < argc; ++i) {
    std::string_view arg{argv[i]};
    if (arg == "--list-tests" || arg == "--list-tags" ||
        arg == "--list-reporters" || arg == "--list-listeners") {
      return true;
    }
  }
  return false;
}

}  // namespace

int main(int argc, char** argv) {
  if (!IsCatchListCommand(argc, argv)) {
    HAL_Initialize(500, 0);
  }
  return Catch::Session().run(argc, argv);
}

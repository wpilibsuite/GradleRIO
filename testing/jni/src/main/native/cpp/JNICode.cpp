#include "frc_robot_JNICode.h"

#include "wpi/jni_util.h"
#include "wpi/raw_ostream.h"

extern "C" {
JNIEXPORT void JNICALL Java_frc_robot_JNICode_jniFunction
  (JNIEnv *, jclass) {
    wpi::outs() << "Hello from JNI!\n";
    wpi::outs().flush();
  }
}

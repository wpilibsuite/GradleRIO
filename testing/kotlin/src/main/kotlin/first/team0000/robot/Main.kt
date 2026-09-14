/*----------------------------------------------------------------------------*/
/* Copyright (c) 2018 FIRST. All Rights Reserved.                             */
/* Open Source Software - may be modified and shared by FIRST teams. The code */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

@file:JvmName("Main")

package first.team0000.robot

import org.wpilib.framework.RobotBase

/**
 * Main initialization function. Do not perform any initialization here.
 *
 * If you change your main robot class, change the parameter type.
 */
fun main() {
    RobotBase.startRobot(::Robot)
}

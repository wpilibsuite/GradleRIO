package edu.wpi.first.gradlerio

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

interface SingletonTask {
    public String singletonName()
}
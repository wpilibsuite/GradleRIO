#!/bin/bash

command -v crew >/dev/null 2>&1 || { bash $(curl https://github.com/skycocker/chromebrew/raw/master/install.sh); crew install git wget}

crew install jdk8
crew install gradle

sudo mount -o remount,exec /
sudo mount -o remount,exec ~/

echo "sudo mount -o remount,exec / && sudo mount -o remount,exec ~/" >> ~/.profile

echo "\033[1;34mUse gradle instead of ./gradlew when working with any GradleRIO projects\033[0m"

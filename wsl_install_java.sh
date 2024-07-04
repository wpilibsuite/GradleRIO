#! /usr/bin/env bash

JAVA_REL=17
JAVA_HOME="/usr/lib/jvm/java-$JAVA_REL-openjdk-amd64"

set -ex
sudo apt-get install -y --no-install-recommends \
	openjdk-$JAVA_REL-jdk-headless
sudo update-alternatives --set java "$JAVA_HOME/bin/java"
sudo update-alternatives --set javac "$JAVA_HOME/bin/javac"
(javac -version | grep -q "javac $JAVA_REL.*") || exit

cat << EOF | sudo tee /etc/profile.d/wpi-openjdk.sh
export JAVA_HOME=$JAVA_HOME
EOF


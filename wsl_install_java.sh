#!/bin/bash

set -ex

# UPDATE THESE URLs
export JDK_URL=http://download.oracle.com/otn-pub/java/jdk/8u131-b11/d54c1d3a095b4ff2b6607d096fa80163/jdk-8u131-linux-x64.tar.gz
export UNLIMITED_STRENGTH_URL=http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip

# Download Oracle Java 8 accepting the license
wget --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" \
${JDK_URL}
# Extract the archive
tar -xzvf jdk-*.tar.gz
# clean up the tar
rm -fr jdk-*.tar.gz
# mk the jvm dir
sudo mkdir -p /usr/lib/jvm
# move the server jre
sudo mv jdk1.8* /usr/lib/jvm/oracle_jdk8

# install unlimited strength policy
wget --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" \
${UNLIMITED_STRENGTH_URL}
unzip jce_policy-8.zip
mv UnlimitedJCEPolicyJDK8/local_policy.jar /usr/lib/jvm/oracle_jdk8/jre/lib/security/
mv UnlimitedJCEPolicyJDK8/US_export_policy.jar /usr/lib/jvm/oracle_jdk8/jre/lib/security/

sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/oracle_jdk8/jre/bin/java 2000
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/oracle_jdk8/bin/javac 2000

sudo echo "export J2SDKDIR=/usr/lib/jvm/oracle_jdk8
export J2REDIR=/usr/lib/jvm/oracle_jdk8/jre
export PATH=$PATH:/usr/lib/jvm/oracle_jdk8/bin:/usr/lib/jvm/oracle_jdk8/db/bin:/usr/lib/jvm/oracle_jdk8/jre/bin
export JAVA_HOME=/usr/lib/jvm/oracle_jdk8
export DERBY_HOME=/usr/lib/jvm/oracle_jdk8/db" | sudo tee -a /etc/profile.d/oraclejdk.sh
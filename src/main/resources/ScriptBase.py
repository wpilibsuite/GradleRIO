#!/usr/bin/env python

from __future__ import print_function
import sys
import os
import subprocess
import time

fullScript = os.path.abspath(sys.argv[0])

scriptName = os.path.basename(sys.argv[0])
scriptName = os.path.splitext(scriptName)[0]

jarName = scriptName + '.jar'

toolsFolder = os.path.dirname(fullScript)

fullJarPath = os.path.join(toolsFolder, jarName)

jdkDir = os.path.dirname(toolsFolder)
jdkDir = os.path.join(jdkDir, 'jdk', 'bin', 'java')

try:
  subProc = subprocess.Popen([jdkDir, '-jar', fullJarPath], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  # If here, start succeeded
except:
  # Start failed, try JAVA_home
  try:
    javaHome = os.environ['JAVA_HOME']
    jdkDir = os.path.join(javaHome, 'bin', 'java')
  except:
    # No JAVA_HOME, try just running java from path
    jdkDir = 'java'
  try:
    subProc = subprocess.Popen([jdkDir, '-jar', fullJarPath], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  except Exception as e:
    # Really error
    print('Error Launching Tool: ')
    print(e)
    exit(1)

# wait 3 seconds, if still open good
count = 0
while subProc.poll() is None:
  time.sleep(1)
  count = count + 1
  if count > 2:
    exit(0)


outputStd = subProc.stdout.read()
outputErr = subProc.stderr.read()

print(outputStd.decode('utf-8'))
print(outputErr.decode('utf-8'), file=sys.stderr)

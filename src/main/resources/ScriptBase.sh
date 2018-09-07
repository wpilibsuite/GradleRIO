#!/bin/bash

SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
SCRIPTFULLNAME=`basename "$0"`
SCRIPTNAME="${SCRIPTFULLNAME%.*}"

JARPATH="$SCRIPTPATH/$SCRIPTNAME.jar"

java -jar $JARPATH >/dev/null 2>&1 &

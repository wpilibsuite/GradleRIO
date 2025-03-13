FRC_PID_FILE=/var/run/natinst/FRC_UserProgram.pid

usage()
{
   cat >&2 <<EOF
        Usage: ${0##*/} [-r] [-t]

        -r      Restart the lvrt process
        -t      Configure lvrt to start a text-based program

EOF
   exit 1
}

while getopts "rt" Option
do
        case $Option in
                r)      RESTART=yes;;
                t)      TEXT_BASED_PROGRAM=yes;;
                *)      usage;;
        esac
done


if [ "$TEXT_BASED_PROGRAM" = yes ]; then
   /usr/local/frc/bin/frcEnableTBLStartupApp.sh
   if [ "$RESTART" = yes ]; then
      nirtcfg --file /etc/natinst/share/lvrt.conf --set section=LVRT,token=RTTarget.LaunchAppAtBoot,value=True
   else
      nirtcfg --file /etc/natinst/share/lvrt.conf --set section=LVRT,token=RTTarget.LaunchAppAtBoot,value=False
   fi
fi

printf "%s - Killing robot code in frcKillRobot.sh\n" "`date`" | FRC_ConsoleTee

nirtcfg --file /etc/natinst/share/lvrt.conf --set section=LVRT,token=RTTarget.LaunchAppAtBoot,value=False

if [ -e $FRC_PID_FILE ]; then
  DAEMON_PID=`cat $FRC_PID_FILE`
  if [ -e /proc/$DAEMON_PID ]; then
    PGROUP=`ps -o pid,pgid,comm | grep $DAEMON_PID | awk -v N=2 '{print $N}'`
    kill -term -- -$PGROUP
    sleep 1
    kill -9 -- -$PGROUP
    sleep 1
  fi
  rm -f $FRC_PID_FILE
fi

if [ "$RESTART" = yes ]; then
   RT_STARTUP_DISABLED=`nirtcfg --get section=SYSTEMSETTINGS,token=NoApp.enabled,value=false | tr "[:upper:]" "[:lower:]"`
   nirtcfg --file /etc/natinst/share/lvrt.conf --set section=LVRT,token=RTTarget.LaunchAppAtBoot,value=True
   if [ "$RT_STARTUP_DISABLED" = false ]; then
      killall lvrt
        else
      /usr/local/frc/bin/frcRunRobot.sh &
   fi
fi

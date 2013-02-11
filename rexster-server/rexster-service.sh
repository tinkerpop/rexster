#!/bin/sh
### BEGIN REDHAT INFO
# chkconfig: 2345 99 20
# description: The Rexster server. See http://rexster.tinkerpop.com
### END REDHAT INFO
### BEGIN INIT INFO
# Provides:          rexster
# Required-Start:
# Required-Stop:
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
### END INIT INFO

# Init script for Rexster so it automatically starts/stops with the machine.
#
# To install:
# 1)  Add a symlink to this file in /etc/init.d/ under the name you'd like to see the service
#     For example, to name the service "rexster": ln -s /rexster/bin/rexster-service.sh /etc/init.d/rexster
# 2a) If you're running RH: chkconfig --add rexster
# 2b) If you're running Ubuntu: update-rc.d rexster defaults
#
# You have to SET the Rexster installation directory here:
REXSTER_DIR="/usr/local/packages/rexster-server"
REXSTER_LOG_DIR="/var/log/rexster"
# Specify the user to run Rexster as:
REXSTER_USER="rexster"
# JAVA_OPTIONS only gets used on start
JAVA_OPTIONS="-Xms64m -Xmx512m"


usage() {
    echo "Usage: `basename $0`: <start|stop|status>"
    exit 1
}

start() {
    status
    if [ $PID -gt 0 ]
    then
        echo "Rexster server has already been started. PID: $PID"
        return $PID
    fi
    export JAVA_OPTIONS
    echo "Starting Rexster server..."
    su -c "cd \"$REXSTER_DIR\"; /usr/bin/nohup ./bin/rexster.sh -s 1>$REXSTER_LOG_DIR/service.log 2>$REXSTER_LOG_DIR/service.err &" $REXSTER_USER
}

stop() {
    status
    if [ $PID -eq 0 ]
    then
        echo "Rexster server has already been stopped."
        return 0
    fi
    echo "Stopping Rexster server..."
    su -c "cd \"$REXSTER_DIR\"; /usr/bin/nohup ./bin/rexster.sh -x 1>$REXSTER_LOG_DIR/service-stop.log 2>$REXSTER_LOG_DIR/service-stop.err &" $REXSTER_USER
}

status() {
    PID=`ps -ef | grep $REXSTER_USER | grep java | grep -v grep | awk '{print $2}'`
    if [ "x$PID" = "x" ]
    then
        PID=0
    fi

    # if PID is greater than 0 then Rexster is running, else it is not
    return $PID
}

if [ "x$1" = "xstart" ]
then
    start
    exit 0
fi

if [ "x$1" = "xstop" ]
then
    stop
    exit 0
fi

if [ "x$1" = "xstatus" ]
then
    status
    if [ $PID -gt 0 ]
    then
        echo "Rexster server is running with PID: $PID"
    else
        echo "Rexster server is NOT running"
    fi
    exit $PID
fi

usage

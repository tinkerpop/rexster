#!/bin/bash

CP=$( echo `dirname $0`/../lib/*.jar . | sed 's/ /:/g')
#echo $CP

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    JAVA_OPTIONS="-Xms32M -Xmx512M"
fi

# Launch the application
$JAVA $JAVA_OPTIONS -cp $CP com.tinkerpop.rexster.WebServer $1

# Return the program's exit code
exit $?

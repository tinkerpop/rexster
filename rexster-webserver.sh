#!/bin/bash

# Path to JAR
JAR=`dirname $0`/target/rexster-*-standalone.jar

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
	JAVA="java"
else
	JAVA="$JAVA_HOME/bin/java"
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
	JAVA_OPTIONS="-Xms64M -Xmx1024M"
fi

# Launch the application
$JAVA $JAVA_OPTIONS -cp $JAR com.tinkerpop.rexster.WebServer $1

# Return the program's exit code
exit $?

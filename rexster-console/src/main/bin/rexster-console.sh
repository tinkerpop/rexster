#!/bin/bash

# From: http://stackoverflow.com/a/246128
#   - To resolve finding the directory after symlinks
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"

CP=$( echo $DIR/../lib/*.jar . | sed 's/ /:/g')
CP=$CP:$( echo $DIR/../ext/*.jar . | sed 's/ /:/g')
#echo $CP

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    JAVA_OPTIONS="-Xms32m -Xmx512m"
fi

# Execute the application and return its exit code
exec $JAVA $JAVA_OPTIONS -cp $CP com.tinkerpop.rexster.console.RexsterConsole $@

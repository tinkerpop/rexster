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
CP=$CP:$(find -L $DIR/../ext/ -name "*.jar" | tr '\n' ':')

REXSTER_EXT=../ext

PUBLIC=$DIR/../public/
EXTRA=

if [ "$1" = "-s" ] ; then
    EXTRA="-wr $PUBLIC"
fi

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
exec $JAVA $JAVA_OPTIONS -cp $CP com.tinkerpop.rexster.Application $@ $EXTRA

#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"
PUBDIR=$DIR/target/rexster-server-*-standalone/public/

$DIR/target/rexster-server-*-standalone/bin/rexster.sh $@ -wr $PUBDIR

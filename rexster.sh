#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"
$DIR/rexster-server/target/rexster-server-*-standalone/bin/rexster.sh $@ -webroot $DIR/rexster-server/target/rexster-server-*-standalone/bin/public

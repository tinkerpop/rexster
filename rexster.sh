#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"
$DIR/target/rexster-*-standalone/bin/rexster.sh $@ -webroot $DIR/target/rexster-*-standalone/bin/public
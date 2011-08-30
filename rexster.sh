#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"
cd $DIR/target/rexster-*-standalone/bin/
bash rexster.sh $@
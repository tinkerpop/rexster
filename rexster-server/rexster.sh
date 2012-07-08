#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"

$DIR/target/rexster-server-*-standalone/bin/rexster.sh $@ 

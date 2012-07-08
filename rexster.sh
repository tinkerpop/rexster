#!/bin/bash

cd rexster-server

DIR="$( cd "$( dirname "$0" )" && pwd )"

$DIR/target/rexster-server-*-standalone/bin/rexster.sh $@ 

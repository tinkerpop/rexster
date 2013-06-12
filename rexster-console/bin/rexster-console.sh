#!/bin/bash

DIR="$( cd "$( dirname "$0" )" && pwd )"

$DIR/../target/rexster-console-*-standalone/bin/rexster-console.sh $@

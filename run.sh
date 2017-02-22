#!/bin/bash
# run the application without putting it into background and redirecting output
# this is useful when running in docker
DIR="$( dirname "$0" )"
cd $DIR
java -jar winstone-0.9.9.jar -Xmx512 -Xms512m
#java -jar winstone-0.9.9.jar -Xmx512 -Xms512m | tee logs/error.log 2>&1
# command > >(tee stdout.log) 2> >(tee stderr.log >&2)


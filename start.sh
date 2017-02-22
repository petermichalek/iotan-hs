#!/bin/bash
# start the application
#scala -jar winstone-0.9.9.jar -Xmx512 -Xms512m  >> out.log 2>&1 &
java -jar winstone-0.9.9.jar -Xmx512 -Xms512m  >> out.log 2>&1 &
sleep 1
tail out.log


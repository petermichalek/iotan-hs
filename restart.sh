#!/bin/bash
# restart server
PID=`lsof out.log |awk '/java/{print $2}'|uniq`
echo kill $PID
kill $PID
sleep 1
./start.sh
sleep 1

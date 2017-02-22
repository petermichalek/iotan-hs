#!/bin/bash
# stop the server
PID=`lsof out.log |awk '/java/{print $2}'|uniq`
echo kill $PID
kill $PID


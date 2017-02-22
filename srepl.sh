#!/bin/bash
# run scala REPL standalone with all jars in classpath
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
LIBDIR=$DIR/WEB-INF/lib
#LIBDIR=lib
#LIBDIR=target/pack/lib
CP=
for f in $LIBDIR/*.jar ; do CP=$CP:$f; done
#echo $CP
scala -cp $CP $@

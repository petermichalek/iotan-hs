#!/bin/bash
# deploy binaries
cp -a ./build/libs/*.jar WEB-INF/lib/
cp -a ../haystack-java/build/libs/haystack-java-3.0.0-SNAPSHOT.jar WEB-INF/lib/
cp -a ../iotan-core/target/pack/lib/*.jar WEB-INF/lib/
# this overrides pack/lib for iotus-core_2.11-0.1.01.jar
cp -a ../iotan-core/target/scala-2.11/*.jar WEB-INF/lib/
#cp /opt/scala/lib/scala-library.jar WEB-INF/lib/


#!/bin/bash
#set -x
onlyIotus=0
if [ $# > 0 ] && [ "$1" == "onlyIotus" ] ; then
  echo onlyIotus
  onlyIotus=1
fi
echo onlyIotus: $onlyIotus
# pack binaries for docker
# ./gradlew build and ./deploy.sh must have been ran before
#version = '0.1.01'
version=$(awk -F\' '/version = / {print $2}' ./build.gradle)
BINDIR=build/iotan-hs-bin-$version
TARNAME_ALL=iotan-hs-bin-all-$version.tar.gz
TARNAME_DEPS=iotan-hs-bin-deps-$version.tar.gz
TARNAME_IOTAN=iotan-hs-bin-$version.tar.gz

# first cleanup from previous build
for f in iotan-hs-bin-dev-$version iotan-hs-bin-all-$version iotan-hs-bin-$version ; do
  rm -rf build/$f
done

mkdir $BINDIR
cp -a *.sh $BINDIR
cp -a winstone.properties winstone*jar $BINDIR
cp -a WEB-INF $BINDIR
cd build

mkdir -p ./iotan-hs-bin-deps-$version/WEB-INF/lib
mkdir ./iotan-hs-bin-all-$version

# all, just replicate the dir
cp -a ./iotan-hs-bin-$version/* ./iotan-hs-bin-all-$version
# deps: only lib dir
mv ./iotan-hs-bin-$version/WEB-INF/lib/* ./iotan-hs-bin-deps-$version/WEB-INF/lib
# iotan: all minus non-iotan libs delete from lib/*
mv ./iotan-hs-bin-deps-$version/WEB-INF/lib/iotus* ./iotan-hs-bin-$version/WEB-INF/lib/

echo tar zcvf ../$TARNAME_IOTAN ./iotan-hs-bin-$version/
tar zcvf ../$TARNAME_IOTAN ./iotan-hs-bin-$version/
if [ $onlyIotus != 0 ] ; then
    cd ..
    echo 1 new file was created iotus-only:
    ls -l $TARNAME_IOTAN
else
    echo tar zcvf ../$TARNAME_ALL ./iotan-hs-bin-all-$version
    tar zcvf ../$TARNAME_ALL ./iotan-hs-bin-all-$version
    echo tar zcvf ../$TARNAME_DEPS ./iotan-hs-bin-deps-$version
    tar zcvf ../$TARNAME_DEPS ./iotan-hs-bin-deps-$version
    cd ..
    echo 3 new files were created for all, deps, iotus-only:
    ls -l $TARNAME_ALL $TARNAME_DEPS $TARNAME_IOTAN
fi



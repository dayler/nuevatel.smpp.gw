#! /bin/sh
SOURCE_LIB_DIR=~/dev/svn/com/nuevatel/mc/trunk/src
LIB_DIR=~/dev/svn/com/nuevatel/mc/trunk/debug/mc.smpp.gw/lib

echo "Updating third party libs...."
cd ${LIB_DIR}
## open-smpp
## cp -apv /Users/asalazar/dev/git/github/opensmpp/core/target/*.jar .
## cp -apv /Users/asalazar/dev/git/github/opensmpp/charset/target/*.jar .
## common
## cp -apv ${SOURCE_DIR}/common/trunk/core/target/*.jar .
## appconn
cp -apvL ${SOURCE_LIB_DIR}/appconn/dist/appconn.jar .
## mc.common
cp -apvL ${SOURCE_LIB_DIR}/mc.common/dist/mc.common.jar .
## wsconn
cp -apvL ${SOURCE_LIB_DIR}/wsconn/dist/wsconn.jar .

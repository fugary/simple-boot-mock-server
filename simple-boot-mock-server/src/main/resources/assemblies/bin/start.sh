#!/usr/bin/env bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
echo -e "Starting the @project.name@ ...\n"
JAVA_OPTS="-Xms512M -Xmx512M -XX:MaxPermSize=128M"
java $JAVA_OPTS -jar $DEPLOY_DIR/@project.build.finalName@.jar &
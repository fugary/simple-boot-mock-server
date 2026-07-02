@echo off
cd ..
title @project.name@
echo Starting the @project.name@ ...
set JAVA_OPTS=-Xms512M -Xmx1024M
set MOCK_DB_H2_CONSOLE=true
set MOCK_DB_DATA_DIR=./data
java %JAVA_OPTS% -Dfile.encoding=UTF-8 -jar @project.build.finalName@.jar

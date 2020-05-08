cd ..
title @project.name@
echo Starting the @project.name@ ...
set JAVA_OPTS=-Xms512M -Xmx512M -XX:MaxPermSize=128M
java %JAVA_OPTS% -jar @project.build.finalName@.jar

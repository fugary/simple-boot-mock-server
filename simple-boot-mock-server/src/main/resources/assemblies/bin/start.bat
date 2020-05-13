cd ..
title @project.name@
echo Starting the @project.name@ ...
set JAVA_OPTS=-Xms256M -Xmx512M
java %JAVA_OPTS% -jar @project.build.finalName@.jar

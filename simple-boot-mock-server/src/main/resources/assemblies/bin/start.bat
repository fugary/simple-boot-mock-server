cd ..
title @project.name@
echo Starting the @project.name@ ...
set JAVA_OPTS=-Xms256M -Xmx512M
set MOCK_DB_H2_CONSOLE=true
java %JAVA_OPTS% -Dfile.encoding=UTF-8 -jar @project.build.finalName@.jar

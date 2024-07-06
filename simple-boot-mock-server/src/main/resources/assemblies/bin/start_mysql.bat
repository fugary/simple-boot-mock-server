cd ..
title @project.name@
echo Starting the @project.name@ ...
set JAVA_OPTS=-Xms256M -Xmx512M
set MOCK_DB_TYPE=mysql
set MOCK_DB_MYSQL_SERVER=localhost
set MOCK_DB_MYSQL_PORT=3306
set MOCK_DB_MYSQL_DBNAME=mock-db
set MOCK_DB_USERNAME=root
set MOCK_DB_PASSWORD=12345678
java %JAVA_OPTS% -Dfile.encoding=UTF-8 -jar @project.build.finalName@.jar

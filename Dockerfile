# Start with base image
FROM adoptopenjdk/openjdk11:alpine-jre

# Add Maintainer Info
LABEL maintainer="fugary"

# Add a temporary volume
VOLUME /tmp

# Expose Port 9086
EXPOSE 9086

ENV JAVA_OPTS="-Xmx512M"
# 类型支持h2和mysql
ENV MOCK_DB_TYPE="h2"
# h2数据库
ENV MOCK_DB_DATA_DIR="/data"
ENV MOCK_DB_H2_CONSOLE=false
#MySQL数据库
ENV MOCK_DB_MYSQL_SERVER='localhost'
ENV MOCK_DB_MYSQL_PORT=3306
ENV MOCK_DB_MYSQL_DBNAME=mock-db
#通用
ENV MOCK_DB_USERNAME="root"
ENV MOCK_DB_PASSWORD="12345678"
ENV MOCK_DB_POOL_SIZE=5
EXPOSE 9086

# Application Jar File
ARG JAR_FILE=simple-boot-mock-server/target/simple-boot-mock-server*.jar

# Add Application Jar File to the Container
ADD ${JAR_FILE} simple-boot-mock-server.jar

# Run the JAR file
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /simple-boot-mock-server.jar"]

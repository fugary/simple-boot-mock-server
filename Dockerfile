# Start with base image
FROM openjdk:8-jdk-alpine

# Add Maintainer Info
LABEL maintainer="fugary"

# Add a temporary volume
VOLUME /tmp

# Expose Port 9086
EXPOSE 9086

ENV JAVA_OPTS="-Xmx512M"
ENV MOCK_DATA_DIR="/data"

# Application Jar File
ARG JAR_FILE=simple-boot-mock-server/target/simple-boot-mock-server*.jar

# Add Application Jar File to the Container
ADD ${JAR_FILE} simple-boot-mock-server.jar

# Run the JAR file
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /simple-boot-mock-server.jar"]

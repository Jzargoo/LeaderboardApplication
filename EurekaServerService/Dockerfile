FROM gradle:8-jdk17 AS build
LABEL authors="jzargo"

WORKDIR /home/eurekaServerService

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew clean bootJar -x test

FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /home/eurekaServerService/build/libs/*.jar app.jar
EXPOSE 8761

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java","-jar","app.jar"]

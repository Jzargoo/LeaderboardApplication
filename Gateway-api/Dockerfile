FROM gradle:8.4.1-jdk17 AS build
LABEL authors="jzargo"

WORKDIR /home/gatewayService

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew clean bootJar -x test

FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY --from=build /home/gatewayService/build/libs/*.jar app.jar
EXPOSE 8080

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

ENTRYPOINT ["java","-jar","app.jar"]

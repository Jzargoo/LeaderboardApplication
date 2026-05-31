FROM openjdk:21-slim as build

LABEL authors="jzargo"

WORKDIR application

COPY build/libs/application.jar application.jar

RUN java Djarmode=layertools -jar application.jar extract

FROM  openjdk:21-slim

WORKDIR application

COPY --from=build ./application/dependencies/ ./
COPY --from=build ./application/spring-boot-loader/ ./
COPY --from=build ./application/shapshot-dependencies/ ./
COPY --from=build ./application/application/ ./

ENTRYPOINT["java", "org.springframework.boot.loader.JarLauncher"]
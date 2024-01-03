#FROM gradle:7-jdk-alpine AS build
#COPY --chown=gradle:gradle . /home/gradle/src
#WORKDIR /home/gradle/src
#RUN gradle build --no-daemon
#
#FROM bellsoft/liberica-openjdk-alpine:15.0.1-9
#COPY --from=build /home/gradle/src/build/libs/*.jar /app/fenix-0.0.1-SNAPSHOT.jar
#EXPOSE 9000
#ENTRYPOINT ["java","-jar","/app/fenix-0.0.1-SNAPSHOT.jar"]

FROM mcr.microsoft.com/windows/servercore:ltsc2019 AS build
COPY . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM bellsoft/liberica-openjdk-alpine:15.0.1-9
COPY --from=build /home/gradle/src/build/libs/*.jar /app/fenix-0.0.1-SNAPSHOT.jar
EXPOSE 9000
ENTRYPOINT ["java","-jar","/app/fenix-0.0.1-SNAPSHOT.jar"]
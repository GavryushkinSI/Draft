 Use a newer Gradle version that supports Java 21
FROM gradle:8-jdk-alpine AS build

# Copy the project files to the container
COPY --chown=gradle:gradle . /home/gradle/src

# Set the working directory
WORKDIR /home/gradle/src

# Build the project
RUN gradle build --no-daemon

# Use a compatible JDK for running the application
FROM bellsoft/liberica-openjdk-alpine:15.0.1-9

# Copy the built JAR file to the final image
COPY --from=build /home/gradle/src/build/libs/*.jar /app/fenix-0.0.1-SNAPSHOT.jar

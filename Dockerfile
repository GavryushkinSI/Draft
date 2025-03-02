FROM gradle:8-jdk-alpine AS build

# Copy the project files to the container
COPY --chown=gradle:gradle . /home/gradle/src

# Set the working directory
WORKDIR /home/gradle/src

# Build the project
RUN gradle build --no-daemon

FROM bellsoft/liberica-openjdk-alpine:21

# Copy the built JAR file to the final image
COPY --from=build /home/gradle/src/build/libs/*.jar /app/fenix-0.0.1-SNAPSHOT.jar

# Expose the application port
EXPOSE 9000

# Set the entry point for the application
ENTRYPOINT ["java", "-jar", "/app/fenix-0.0.1-SNAPSHOT.jar"]
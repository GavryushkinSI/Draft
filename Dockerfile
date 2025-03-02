FROM gradle:7-jdk-alpine AS build

# Копируем проект в контейнер
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Очистка кэша Gradle перед сборкой
RUN rm -rf /home/gradle/.gradle/caches/

# Указываем совместимость с Java 15
RUN echo "sourceCompatibility = '15'" >> build.gradle
RUN echo "targetCompatibility = '15'" >> build.gradle

# Выполняем сборку
RUN gradle build --no-daemon --refresh-dependencies

FROM bellsoft/liberica-openjdk-alpine:15.0.1-9

# Копируем собранный JAR файл в новый образ
COPY --from=build /home/gradle/src/build/libs/*.jar /app/fenix-0.0.1-SNAPSHOT.jar

# Открываем порт
EXPOSE 9000

# Указываем команду для запуска приложения
ENTRYPOINT ["java", "-jar", "/app/fenix-0.0.1-SNAPSHOT.jar"]
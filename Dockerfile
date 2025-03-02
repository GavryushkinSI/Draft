FROM gradle:7-jdk-alpine AS build

# Копируем проект в контейнер
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src

# Очистка кэша Gradle перед сборкой
RUN rm -rf /home/gradle/.gradle/caches/

# Выполняем сборку
RUN gradle build --no-daemon --refresh-dependencies

FROM bellsoft/liberica-openjdk-alpine:21

# Копируем собранный JAR файл в новый образ
COPY --from=build /home/gradle/src/build/libs/*.jar /app/fenix-0.0.1-SNAPSHOT.jar

# Открываем порт
EXPOSE 9000

# Указываем команду для запуска приложения
ENTRYPOINT ["java", "-jar", "/app/fenix-0.0.1-SNAPSHOT.jar"]
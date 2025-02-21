# Используем официальный образ OpenJDK 17
FROM openjdk:17-jdk-slim as builder

# Устанавливаем рабочую директорию
WORKDIR /app

# Используем переменную для имени JAR-файла
ARG JAR_FILE=target/rest_api_calculation_penalty-0.0.1-SNAPSHOT.jar

# Копируем JAR-файл в контейнер
COPY ${JAR_FILE} app.jar

# Используем минимальный базовый образ
FROM openjdk:17-jdk-slim

# Создаем пользователя без root-прав
RUN useradd -m appuser
USER appuser

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем JAR-файл из предыдущего этапа
COPY --from=builder /app/app.jar app.jar

# Открываем порт
EXPOSE 8082

# Команда для запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]

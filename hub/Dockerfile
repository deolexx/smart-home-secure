# Етап 1: Збірка (Build stage)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Копіюємо конфігурації для кешування залежностей
COPY pom.xml .
COPY hub/pom.xml ./hub/
COPY device-simulator/pom.xml ./device-simulator/

# Завантажуємо залежності (тільки для модуля hub та його предків)
RUN mvn dependency:go-offline -f pom.xml -pl hub -am

# Копіюємо вихідний код
COPY hub/src ./hub/src

# Збірка додатку
RUN mvn clean package -f pom.xml -pl hub -am -DskipTests

# Етап 2: Рантайм (Runtime stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Встановлюємо wget для healthcheck
RUN apk add --no-cache wget

# Копіюємо тільки фінальний JAR файл з етапу build
# Використовуємо wildcard для копіювання, а потім перейменовуємо для стабільності ENTRYPOINT
COPY --from=build /app/hub/target/hub-*.jar ./app.jar

# Видаляємо original jar, якщо він випадково потрапив (опціонально, залежить від wildcard)
RUN [ -f app-original.jar ] && rm app-original.jar || true

# Налаштування порту
EXPOSE 8080

# Health check (вимагає залежності spring-boot-starter-actuator у проекті)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Запуск додатку з оптимізацією для контейнерів
# -XX:+UseContainerSupport допомагає JVM правильно бачити ліміти пам'яті контейнера
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

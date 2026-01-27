
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY hub/pom.xml ./hub/
COPY device-simulator/pom.xml ./device-simulator/

RUN mvn dependency:go-offline -f pom.xml -pl hub -am

COPY hub/src ./hub/src

RUN mvn clean package -f pom.xml -pl hub -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app


RUN apk add --no-cache wget


COPY --from=build /app/hub/target/hub-*.jar ./app.jar


RUN [ -f app-original.jar ] && rm app-original.jar || true

# Налаштування порту
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1


ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

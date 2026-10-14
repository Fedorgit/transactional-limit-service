FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /workspace/target/transactional-limit-service-0.0.1.jar app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ─── Этап 1: Сборка ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw package -DskipTests -Dcheckstyle.skip -B

# ─── Этап 2: Финальный образ ──────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN mkdir -p uploads/covers uploads/chapters

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT:-8080}"]
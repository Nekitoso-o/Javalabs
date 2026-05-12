                                      # ─── Этап 1: Сборка ───────────────────────────────────────────────
                                      FROM eclipse-temurin:21-jdk-alpine AS builder

                                      WORKDIR /app

                                      # Копируем Maven Wrapper и pom.xml отдельно — Docker кэширует этот слой.
                                      # Зависимости не будут скачиваться заново, если pom.xml не изменился.
                                      COPY .mvn/ .mvn/
                                      COPY mvnw pom.xml ./

                                      # Делаем mvnw исполняемым (важно для Linux-контейнеров)
                                      RUN chmod +x mvnw

                                      # Скачиваем зависимости (кэшируемый слой)
                                      RUN ./mvnw dependency:go-offline -B

                                      # Копируем исходный код и собираем JAR (без тестов для ускорения)
                                      COPY src ./src
                                      RUN ./mvnw package -DskipTests -B

                                      # ─── Этап 2: Финальный образ ──────────────────────────────────────
                                      # Используем JRE (не JDK) — образ в 2 раза меньше
                                      FROM eclipse-temurin:21-jre-alpine

                                      WORKDIR /app

                                      # Создаём папки для загружаемых файлов (обложки и главы)
                                      RUN mkdir -p uploads/covers uploads/chapters

                                      # Копируем только JAR из этапа сборки
                                      COPY --from=builder /app/target/*.jar app.jar

                                      # Открываем порт
                                      EXPOSE 8080

                                      # Запускаем приложение
                                      ENTRYPOINT ["java", "-jar", "app.jar"]
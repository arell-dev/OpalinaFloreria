FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /workspace/target/opalina-*.jar /app/opalina.jar

ENV SPRING_PROFILES_ACTIVE=railway
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/opalina.jar"]

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw ./
COPY .mvn ./.mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

# Cache dependencies in their own layer — only re-downloaded when pom.xml changes
COPY pom.xml ./
RUN ./mvnw -B -q dependency:go-offline || true

COPY src ./src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache ffmpeg
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

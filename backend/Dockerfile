FROM maven:3.9.9-eclipse-temurin-17 AS backend-builder
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
COPY mvnw ./mvnw
COPY mvnw.cmd ./mvnw.cmd
RUN mvn clean package -DskipTests -B


FROM eclipse-temurin:17-jre-alpine AS backend
WORKDIR /app

RUN apk add --no-cache wget
RUN addgroup -S code-titans && adduser -S code-titans -G code-titans

COPY --from=backend-builder /app/target/*.jar app.jar
RUN chown code-titans:code-titans app.jar

USER code-titans
EXPOSE 8082


ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]


FROM nginx:alpine AS frontend
RUN apk add --no-cache wget
COPY nginx /usr/share/nginx/html
EXPOSE 80


CMD ["nginx", "-g", "daemon off;"]

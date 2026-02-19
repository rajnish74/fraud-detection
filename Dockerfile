# ---------- Stage 1: Build ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests


# ---------- Stage 2: Run ----------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/money-muling-engine-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java","-jar","app.jar"]

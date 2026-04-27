FROM node:22-alpine AS css-build

WORKDIR /ui

COPY package.json package-lock.json* ./
RUN npm install

COPY src/main/tailwind ./src/main/tailwind
COPY src/main/resources/templates ./src/main/resources/templates
COPY src/main/resources/static ./src/main/resources/static

RUN npm run build:css

FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
COPY --from=css-build /ui/src/main/resources/static/css/tailwind.css ./src/main/resources/static/css/tailwind.css

RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
# ===== Build stage =====
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package
RUN mvn -q -DskipTests dependency:copy-dependencies -DoutputDirectory=target/libs

# ===== Runtime stage =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# app classes + dependencies
COPY --from=build /app/target/classes /app/classes
COPY --from=build /app/target/libs     /app/libs

# helpful default classpath env
ENV CP="/app/classes:/app/libs/*"

# default command is a no-op; we override per service in docker-compose
CMD ["sh", "-c", "echo 'Set service command in docker-compose.yml' && sleep infinity"]

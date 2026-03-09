
# STAGE 1: BUILD
# Menggunakan image Maven lengkap hanya untuk proses build.
# Image ini TIDAK ikut ke production — hanya untuk compile.
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

ncy.

COPY pom.xml .

RUN mvn dependency:go-offline -B

di atas
COPY src ./src

RUN mvn clean package -DskipTests





RUN addgroup -S appgroup && adduser -S appuser -G appgroup

#


VOLUME /app/uploads

RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", \
 
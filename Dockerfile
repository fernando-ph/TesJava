
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




#
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

#

#
VOLUME /app/uploads

# Ubah ownership file ke appuser
RUN chown appuser:appgroup app.jar
USER appuser

# Port yang di-expose oleh aplikasi Spring Boot
EXPOSE 8080

# JVM flags untuk environment container:
#   -XX:+UseContainerSupport    → JVM baca limit CPU/RAM dari Docker (bukan host)
#   -XX:MaxRAMPercentage=75.0   → JVM pakai maksimal 75% dari RAM yang dialokasikan Docker
#   -Djava.security.egd=...     → Mempercepat startup (random number generator)
ENTRYPOINT ["java", \
 
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY backend/ .
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안: non-root 유저
RUN addgroup -S tradev && adduser -S tradev -G tradev

COPY --from=build /app/build/libs/*.jar app.jar
RUN chown tradev:tradev app.jar

USER tradev
EXPOSE 8080

# JVM 메모리 설정 (t3.small: 2GB RAM → 512MB heap)
ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=60.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

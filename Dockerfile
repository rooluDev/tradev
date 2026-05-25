FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안: non-root 유저 + 로그 디렉토리 생성
RUN addgroup -S tradev && adduser -S tradev -G tradev && \
    mkdir -p /app/logs && \
    chown -R tradev:tradev /app

# CI에서 빌드된 JAR 복사 (프론트엔드 정적 파일 포함)
COPY backend/build/libs/*.jar app.jar
RUN chown tradev:tradev app.jar

USER tradev
EXPOSE 8080

# JVM 메모리 설정 (t3.micro: 1GB RAM)
ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=60.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]

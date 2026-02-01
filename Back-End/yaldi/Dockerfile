# Back-End/yaldi/Dockerfile

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tzdata wget && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime

COPY build/libs/*.jar app.jar

RUN mkdir -p /app/logs

EXPOSE 8080

# JVM 최적화 실행 (2.5GB limit 기준)
ENTRYPOINT ["java", \
    "-Xmx2g", \
    "-Xms512m", \
    "-XX:+UseG1GC", \
    "-XX:G1HeapRegionSize=16m", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=80.0", \
    "-XX:+OptimizeStringConcat", \
    "-XX:+UseStringDeduplication", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
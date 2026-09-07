# 1GB RAM VM에서 직접 빌드하면 OOM 위험이 크므로, jar는 로컬에서 미리 빌드한다.
#   로컬: ./gradlew bootJar  →  build/libs/*.jar 생성
# 이 이미지는 그 jar를 실행만 하는 런타임 전용 이미지다.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app
COPY build/libs/*.jar app.jar

EXPOSE 8080

# 컨테이너 메모리 제한(docker-compose의 mem_limit)에 맞춰 힙을 명시적으로 좁게 잡는다.
# JDK의 컨테이너 인식 기본값에만 맡기면 1GB짜리 VM에서는 너무 크게 잡아 OOM이 난다.
ENTRYPOINT ["sh", "-c", "java -Xmx320m -Xss256k -XX:MaxMetaspaceSize=96m -jar app.jar"]

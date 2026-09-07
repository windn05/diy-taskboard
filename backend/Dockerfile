# jar는 GitHub Actions에서 빌드해 넣는다 (1GB VM에서는 Gradle 빌드가 OOM 난다).
# 이 이미지는 그 jar를 실행만 하는 런타임 전용 이미지다.
#
# alpine 대신 jammy를 쓰는 이유: Temurin의 alpine 이미지는 x86_64만 제공된다.
# jammy는 amd64/arm64 둘 다 있어서, E2(x86)에서 A1(ARM)으로 옮길 때 이미지를 그대로 쓴다.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY build/libs/*.jar app.jar

EXPOSE 8080

# 힙 크기는 인스턴스 사양에 따라 달라지므로 compose의 JAVA_OPTS로 넘긴다.
# 컨테이너 인식 기본값에 맡기면 1GB VM에서 너무 크게 잡아 OOM이 난다.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

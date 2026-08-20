# 멀티스테이지 빌드: gradle로 fat jar 생성 후 JRE 이미지에서 실행
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# 의존성 캐시 최적화를 위해 빌드 스크립트 먼저 복사
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon >/dev/null 2>&1 || true
COPY src ./src
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
# 컨테이너 기본 프로파일(운영 시 SPRING_PROFILES_ACTIVE 환경변수로 재정의)
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

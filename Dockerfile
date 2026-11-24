# ===== 1단계: 공장 (JDK = 만드는 도구 포함) =====
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon -x test

# ===== 2단계: 배송 (JRE = 실행만) =====
FROM eclipse-temurin:17-jre
ENV TZ=Asia/Seoul
WORKDIR /app
COPY --from=build /app/build/libs/board-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
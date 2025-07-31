# 1. Java 17 기반 이미지 사용
FROM openjdk:17

# 2. JAR 파일 이름 지정 (빌드 시 jar 경로 넘겨받음)
ARG JAR_FILE=build/libs/*.jar

# 3. JAR 파일을 컨테이너 내부로 복사
COPY ${JAR_FILE} app.jar

# 4. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app.jar"]

FROM openjdk:17

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar", "--spring.config.additional-location=optional:/app/application.yaml"]

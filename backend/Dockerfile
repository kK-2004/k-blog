FROM eclipse-temurin:17-jre

WORKDIR /app

RUN mkdir -p /logs
VOLUME ["/logs"]

COPY target/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]


FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /workspace/target/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8000

ENV SERVER_PORT=8000

ENTRYPOINT ["sh", "-c", "if [ -n \"$FIREBASE_SERVICE_ACCOUNT_JSON_B64\" ]; then printf '%s' \"$FIREBASE_SERVICE_ACCOUNT_JSON_B64\" | base64 -d > /tmp/firebase-service-account.json && export FIREBASE_SERVICE_ACCOUNT_PATH=/tmp/firebase-service-account.json; fi; exec java $JAVA_OPTS -jar /app/app.jar"]
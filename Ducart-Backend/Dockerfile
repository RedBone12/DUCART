FROM maven:3.9.16-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY config ./config
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode --no-transfer-progress -DskipTests -Djacoco.skip=true clean package

FROM eclipse-temurin:17.0.20_8-jre-alpine
ARG RELEASE_ID=development
LABEL org.opencontainers.image.revision=$RELEASE_ID

WORKDIR /app

RUN addgroup -S ducart \
    && adduser -S ducart -G ducart \
    && mkdir -p /app/uploads \
    && chown -R ducart:ducart /app

COPY --from=build --chown=ducart:ducart \
    /workspace/target/Ducart-Backend-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER ducart

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

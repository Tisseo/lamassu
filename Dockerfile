FROM eclipse-temurin:21 AS build

WORKDIR /app

COPY . .

RUN apt-get update && apt-get install -y maven

ARG MAVEN_ARGS="--no-transfer-progress -Dstyle.color=always"
ARG MAVEN_SKIP_ARGS="-P prettierSkip -Dmaven.test.skip=true -Dmaven.source.skip=true"
RUN mvn $MAVEN_ARGS $MAVEN_SKIP_ARGS package

# Deploy image
FROM eclipse-temurin:21

RUN apt-get update && apt-get install -y coreutils

WORKDIR /app

COPY --from=build /app/target/lamassu-1.2.0-SNAPSHOT.jar lamassu.jar

CMD ["java", "-jar", "/app/lamassu.jar"]

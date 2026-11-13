# ---- build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- run ----
# A JDK (not a JRE): the Proof Engine compiles code in-process with javax.tools.JavaCompiler.
FROM eclipse-temurin:21-jdk
# Docker CLI, only used when RUNNER_MODE=docker (each run then gets its own locked-down container)
COPY --from=docker:27-cli /usr/local/bin/docker /usr/local/bin/docker
RUN useradd --system --create-home --uid 10001 codeverdict
WORKDIR /app
COPY --from=build /app/target/codeverdict-1.0.0.jar app.jar
COPY --from=build /app/target/runner-lib/junit-platform-console-standalone.jar runner-lib/junit-platform-console-standalone.jar
ENV JUNIT_JAR=/app/runner-lib/junit-platform-console-standalone.jar \
    RUNNER_WORK_DIR=/tmp/codeverdict-jobs
USER codeverdict
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-jar", "/app/app.jar"]

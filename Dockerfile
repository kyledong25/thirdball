# Render deploys JVM services with Docker. Build the Spring Boot jar separately
# from the small runtime image so Maven and source files are not shipped.
FROM maven:3.9-eclipse-temurin-11 AS build

WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:11-jre-jammy

WORKDIR /app
RUN useradd --system --uid 10001 --create-home thirdball

COPY --from=build /workspace/target/third-ball-api-0.0.1-SNAPSHOT.jar app.jar

USER thirdball
EXPOSE 8080

# Render's free instance has a small memory budget.  Keep heap, metaspace,
# direct buffers, and thread stacks bounded so the kernel does not kill the
# process while Spring and Hibernate initialize.
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx160m -XX:MaxMetaspaceSize=128m -XX:MaxDirectMemorySize=32m -Xss512k"

# application-prod.properties reads Render's PORT at runtime.
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:-} -jar /app/app.jar"]

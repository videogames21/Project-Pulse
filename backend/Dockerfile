# ---- Extract layers from pre-built JAR ----
FROM --platform=linux/amd64 eclipse-temurin:25-jdk AS builder
WORKDIR /app
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN mkdir layers && java -Djarmode=tools -jar application.jar extract --layers --launcher --destination layers

# ---- Run ----
FROM --platform=linux/amd64 eclipse-temurin:25-jdk
WORKDIR /app
COPY --from=builder /app/layers/dependencies/ ./
COPY --from=builder /app/layers/spring-boot-loader/ ./
COPY --from=builder /app/layers/snapshot-dependencies/ ./
COPY --from=builder /app/layers/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
